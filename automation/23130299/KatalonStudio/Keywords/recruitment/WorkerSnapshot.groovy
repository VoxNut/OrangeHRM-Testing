package recruitment

import com.google.gson.Gson
import java.security.MessageDigest

/** Read-only check; never synchronizes into a prepared or running worker. */
class WorkerSnapshot {
    static final List<String> SETTINGS=['hiringManagerName','hiringManagerAlternativeName','interviewerA','interviewerB','cvLimitBytes']
    static org.w3c.dom.Document xml(File file) {
        def factory=javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.setFeature('http://apache.org/xml/features/disallow-doctype-decl',true)
        factory.setXIncludeAware(false);factory.setExpandEntityReferences(false)
        factory.newDocumentBuilder().parse(file)
    }
    static String sha(File file) { MessageDigest.getInstance('SHA-256').digest(file.bytes).encodeHex().toString() }
    static Map check(File project) {
        File marker=new File(project,'asm7-worker.json')
        if (!marker.isFile()) return [state:'READY',mode:'CANONICAL',validation:'No worker marker']
        Map record=new Gson().fromJson(marker.getText('UTF-8'),Map)
        File canonical=new File(record.source.toString()).canonicalFile
        List<String> changed=[]
        Map fingerprints=(record.profileSha256 ?: [:]) + (record.requiredResourceSha256 ?: [:])
        if (!fingerprints) return [state:'STALE_SNAPSHOT',reason:'Old snapshot metadata; prepare a fresh worker']
        fingerprints.each { relative,digest ->
            File local=new File(project,relative.toString()).canonicalFile
            File source=new File(canonical,relative.toString()).canonicalFile
            assert local.toPath().startsWith(project.canonicalFile.toPath()) && source.toPath().startsWith(canonical.toPath()) : 'Snapshot path outside project'
            if (!local.isFile() || !source.isFile() || sha(local)!=digest || sha(source)!=digest) changed << relative.toString()
        }
        changed ? [state:'STALE_SNAPSHOT',files:changed,reason:'Canonical configuration or frozen worker differs; prepare again'] :
            [state:'READY',mode:'FROZEN_WORKER',validation:'Canonical and worker fingerprints agree']
    }
    static Map runtimeSettings(File project,Map loaded,String profile='default') {
        File file=new File(project,'Profiles/'+profile+'.glbl')
        if (!file.isFile()) return [state:'STALE_SNAPSHOT',reason:'Selected profile source is missing; reopen/prepare project']
        Map source=[:]
        def rows=xml(file).getElementsByTagName('GlobalVariableEntity')
        for (int i=0;i<rows.length;i++) {
            def row=rows.item(i)
            String name=row.getElementsByTagName('name').item(0).textContent,raw=row.getElementsByTagName('initValue').item(0).textContent
            if (name in SETTINGS) {
                if (name=='cvLimitBytes' && raw==~ /-?\d+L?/) source[name]=raw.replaceFirst(/L$/,'').toLong()
                else if (raw.size()>=2 && raw.startsWith("'") && raw.endsWith("'")) source[name]=raw.substring(1,raw.length()-1).replace("\\'","'").replace('\\\\','\\')
            }
        }
        List<String> changed=source.findAll { name,value -> loaded[name]!=value }.keySet().toList()
        changed ? [state:'STALE_SNAPSHOT',files:['Profiles/'+profile+'.glbl'],settings:changed,reason:'Loaded GlobalVariable differs from selected profile source. Refresh/reopen/build; prepare worker again. No GlobalVariable was overwritten.'] : [state:'READY',validation:'Loaded settings match profile literals']
    }
}
