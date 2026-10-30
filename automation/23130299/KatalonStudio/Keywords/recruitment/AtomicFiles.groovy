package recruitment

import com.google.gson.GsonBuilder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.StandardCopyOption

/** Destinations belong to one execution; historical evidence is never overwritten. */
class AtomicFiles {
    static void json(File target,Object value,boolean replace=true) {
        bytes(target,(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(value)+'\n').getBytes('UTF-8'),replace)
    }
    static void bytes(File target,byte[] value,boolean replace=false) {
        Files.createDirectories(target.parentFile.toPath())
        if (!replace && target.exists()) throw new java.nio.file.FileAlreadyExistsException(target.path)
        File staging=new File(target.parentFile,'.'+target.name+'.'+UUID.randomUUID()+'.tmp')
        try {
            Files.write(staging.toPath(),value,StandardOpenOption.CREATE_NEW)
            FileChannel channel=FileChannel.open(staging.toPath(),StandardOpenOption.WRITE)
            try { channel.force(true) } finally { channel.close() }
            if (replace) Files.move(staging.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
            else Files.move(staging.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE)
        } finally { Files.deleteIfExists(staging.toPath()) } // only this call's staging file
    }
}
