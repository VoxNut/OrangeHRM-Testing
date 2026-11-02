package recruitment

import com.kms.katalon.core.configuration.RunConfiguration
import java.nio.file.*

/** All relative configuration is resolved against the Katalon project, never the shell cwd. */
class PortablePaths {
    static Path project() { Paths.get(RunConfiguration.getProjectDir()).toAbsolutePath().normalize() }
    static File resolve(String configured, String fallback, Path base=project()) {
        Path value=Paths.get(configured?.trim() ?: fallback)
        (value.isAbsolute() ? value : base.resolve(value)).normalize().toFile()
    }
    static File evidence(Path base=project(), Map environment=System.getenv()) {
        String override=environment['ASM7_EVIDENCE_ROOT']?.toString()?.trim()
        if (override) {
            Path path=Paths.get(override)
            if (!path.isAbsolute() || path.normalize()==path.root || path.normalize()==base.normalize())
                throw new IllegalArgumentException('ASM7_EVIDENCE_ROOT must be an absolute dedicated output directory')
            // Shared group evidence may only target this member's namespace.
            String normalized=path.normalize().toString().replace('\\','/')
            if (normalized =~ /(?i)\/evidence\/executions\/(?!23130299(?:\/|$))[^\/]+/)
                throw new IllegalArgumentException('ASM7_EVIDENCE_ROOT targets a different member')
            return path.normalize().toFile()
        }
        Path normalized=base.toAbsolutePath().normalize()
        // Only the known member project placement qualifies; an arbitrary ancestor .git is insufficient.
        if (normalized.fileName?.toString()=='KatalonStudio' && normalized.parent?.fileName?.toString()=='23130299' &&
            normalized.parent?.parent?.fileName?.toString()=='automation') {
            Path repo=normalized.parent.parent.parent
            if (Files.exists(repo.resolve('.git'))) return repo.resolve('evidence/executions/23130299').toFile()
        }
        normalized.resolve('evidence/executions/23130299').toFile()
    }
}
