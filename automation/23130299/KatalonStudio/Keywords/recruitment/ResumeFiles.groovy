package recruitment

import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Synthetic local assets generated only during an owner-initiated run, never real CVs. */
class ResumeFiles {
    static String hintLocator() {
        return RecruitmentUi.group('Resume') + "/ancestor::div[${RecruitmentUi.cls('orangehrm-file-input')}][1]//*[${RecruitmentUi.cls('orangehrm-input-hint')}]"
    }
    String readHint() { return ui.one(hintLocator()).text.replace('\u00a0', ' ').trim().replaceAll(/\s+/, ' ') }
    Map readRule() {
        String hint = readHint()
        Map rule = smallFileRule(hint)
        if (!rule) ui.blocked('Resume format/size hint unavailable at the verified file-component locator: ' + hintLocator())
        return rule + [uiHint:hint]
    }
    void requireBoundaryConfig() {
        long bytes = (RecruitmentUi.config('cvLimitBytes', 0) as Number).longValue()
        if (bytes <= 0 || bytes > 50L * 1024L * 1024L) ui.blocked('Set cvLimitBytes from the live Resume size rule (exact bytes), not a commercial-version default.')
    }
    final CandidateChecks ui
    final File directory
    ResumeFiles(CandidateChecks ui) {
        this.ui = ui
        directory = new File(ui.output, 'assets')
        assert directory.mkdirs() || directory.isDirectory()
    }
    /** Local asset checks only; does not instantiate Katalon or open a browser. */
    ResumeFiles(File directory) {
        this.ui = null
        this.directory = directory
        assert directory.mkdirs() || directory.isDirectory()
    }
    File pdf(String name = 'resume_run.pdf') {
        File file = new File(directory, name)
        String text = 'BT /F1 12 Tf 40 750 Td (ASM7 synthetic CV - 23130299) Tj ET'
        List<String> objects = [
            '<< /Type /Catalog /Pages 2 0 R >>',
            '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
            '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
            '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
            '<< /Length ' + text.length() + ' >>\nstream\n' + text + '\nendstream'
        ]
        StringBuilder data = new StringBuilder('%PDF-1.4\n')
        List<Integer> offsets = []
        objects.eachWithIndex { obj, i -> offsets << data.length(); data.append((i + 1) + ' 0 obj\n' + obj + '\nendobj\n') }
        int xref = data.length()
        data.append('xref\n0 6\n0000000000 65535 f \n')
        offsets.each { data.append(String.format('%010d 00000 n \n', it)) }
        data.append('trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n' + xref + '\n%%EOF\n')
        file.bytes = data.toString().getBytes(StandardCharsets.US_ASCII)
        assert file.text.startsWith('%PDF-1.4') && file.text.endsWith('%%EOF\n')
        return file
    }
    File docx() {
        File file = new File(directory, 'resume_run.docx')
        Map<String,String> entries = [
            '[Content_Types].xml':'<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>',
            '_rels/.rels':'<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>',
            'word/document.xml':'<?xml version="1.0"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>ASM7 synthetic CV - 23130299</w:t></w:r></w:p><w:sectPr/></w:body></w:document>'
        ]
        file.withOutputStream { raw ->
            ZipOutputStream zip = new ZipOutputStream(raw)
            entries.each { path, xml -> zip.putNextEntry(new ZipEntry(path)); zip.write(xml.getBytes('UTF-8')); zip.closeEntry() }
            zip.finish()
        }
        assert file.bytes[0..1] == [80, 75]
        return file
    }
    File invalidExtension() {
        File file = new File(directory, 'resume_run.exe')
        file.setText('Inert text fixture for extension validation. This is not executable code.\n', 'UTF-8')
        return file
    }
    /** Small non-boundary files only: use the lower of the UI's possible byte conventions.
     * This is NOT an assertion of the demo's exact MB convention. Only limit() can supply that.
     */
    static Map smallFileRule(String hint) {
        def match = hint =~ /(?i)(\d+(?:\.\d+)?)\s*(KiB|MiB|KB|MB|bytes?)/
        if (!match.find()) return null
        BigDecimal amount = new BigDecimal(match.group(1))
        String unit = match.group(2).toLowerCase()
        List<Long> scales = unit == 'mib' ? [1048576L] : unit == 'kib' ? [1024L] :
            unit == 'mb' ? [1000000L, 1048576L] : unit == 'kb' ? [1000L, 1024L] : [1L]
        long lowerBound = scales.collect { (amount * it).longValueExact() }.min()
        List<String> extensions = (hint.toLowerCase() =~ /\.(docx|doc|odt|pdf|rtf|txt)\b/).collect { it[1] }.unique()
        return lowerBound > 0 && extensions ? [lowerBoundBytes:lowerBound, extensions:extensions, exactLimitVerified:false] : null
    }
    static boolean smallFileAllowed(long bytes, String extension, Map rule, boolean supported) {
        return rule != null && bytes > 0 && bytes < rule.lowerBoundBytes &&
            (rule.extensions.contains(extension.toLowerCase()) == supported)
    }
    void checkSmall(File file, boolean supported = true) {
        Map rule = readRule()
        String extension = file.name.tokenize('.').last()
        assert file.isFile() && smallFileAllowed(file.length(), extension, rule, supported) :
            'Resume fixture must have the intended format and be below every byte interpretation of the displayed limit'
        ui.track([kind:'resume-small-file-check', filename:file.name, bytes:file.length(), supportedFormat:supported,
            lowerBoundBytes:rule.lowerBoundBytes, exactLimitVerified:false, uiHint:rule.uiHint])
    }
    long limit() {
        requireBoundaryConfig()
        long limit = (RecruitmentUi.config('cvLimitBytes', 0) as Number).longValue()
        if (limit <= 0 || limit > 50L * 1024L * 1024L) ui.blocked('Set cvLimitBytes from the live Resume size rule (exact bytes), not a commercial-version default.')
        String hint = readHint()
        def match = hint =~ /(?i)(\d+(?:\.\d+)?)\s*(KiB|MiB|KB|MB|bytes?)/
        if (!match.find()) ui.blocked('Cannot read size/unit from Resume UI hint. Inspect locator and limit before upload.')
        assert sourceLimitDisplayMatches(limit,hint) : 'Owner-confirmed CV byte limit disagrees with source-formatted UI hint: ' + hint
        ui.track([kind:'resume-limit', bytes:limit, uiHint:hint, source:'Owner-confirmed byte convention; live rule still under test'])
        return limit
    }
    /** app.js formats maxFileSize/1048576 rounded to two decimals; this checks a supplied L, never derives L. */
    static boolean sourceLimitDisplayMatches(long bytes,String hint) {
        if(bytes<=0)return false
        def match=hint =~ /(?i)(\d+(?:\.\d+)?)\s*(MiB|MB|bytes?)/
        if(!match.find())return false
        BigDecimal shown=new BigDecimal(match.group(1))
        if(match.group(2).toLowerCase().startsWith('byte'))return shown.compareTo(new BigDecimal(bytes))==0
        new BigDecimal(bytes).divide(new BigDecimal(1048576),2,java.math.RoundingMode.HALF_UP).compareTo(shown)==0
    }
    File oversized(long limit) {
        File file = pdf('resume_oversize.pdf')
        assert file.length() < limit : 'CV limit is smaller than the generated PDF'
        // Put an inert PDF comment before xref, preserving object offsets and a final EOF marker.
        // Trailing zero padding after %%EOF could turn a size-boundary fixture into a malformed PDF.
        String original = file.getText('US-ASCII')
        def marker = original =~ /startxref\n(\d+)\n%%EOF/
        assert marker.find()
        int oldXref = Integer.parseInt(marker.group(1))
        long target = limit + 1L
        int padding = (int)(target - original.length())
        for (int i = 0; i < 4; i++) {
            int delta = String.valueOf(oldXref + padding).length() - String.valueOf(oldXref).length()
            padding = (int)(target - original.length() - delta)
        }
        assert padding >= 2
        String comment = '%' + (' ' * (padding - 2)) + '\n'
        String tail = original.substring(oldXref).replaceFirst(/startxref\n\d+/, 'startxref\n' + (oldXref + padding))
        file.setText(original.substring(0, oldXref) + comment + tail, 'US-ASCII')
        assert file.length() == limit + 1L
        return file
    }
}
