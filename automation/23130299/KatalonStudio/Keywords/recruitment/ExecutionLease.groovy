package recruitment

/** Compatibility facade. No project-wide lock or admission gate in test listeners/helpers. */
class ExecutionLease {
    static void acquire(boolean suite=false, File project=null) {
        if (suite) ExecutionContext.start('',true,project ? new File(project,'runtime/ASM7') : null)
        else if (!ExecutionContext.peek()) ExecutionContext.start('',false,project ? new File(project,'runtime/ASM7') : null)
    }
    static boolean owns() { ExecutionContext.peek()!=null }
    static boolean isSuite() { ExecutionContext.peek()?.suiteExecution==true }
    static void release() {
        ExecutionContext c=ExecutionContext.peek()
        if (c) c.end()
        ExecutionContext.detach()
    }
}
