package recruitment

/** Authentication belongs to the current execution, never to the project. */
class SessionGuard {
    static boolean getSuiteActive() { ExecutionContext.peek()?.suiteExecution==true }
    static boolean getExhausted() { ExecutionContext.current().authentication.exhausted==true }
    static void startSuite() { ExecutionContext.current().authentication=[authenticated:false,exhausted:false,reason:''] }
    static void endSuite() { ExecutionContext.detach() }
    static void startCase() { ExecutionContext.current() }
    static void requireAllowance() {
        if (exhausted) throw new IllegalStateException('[ENVIRONMENT / AUTHENTICATION BLOCKED] This execution lost login or timed out. Other executions are unaffected; start a fresh execution after restoring login.')
    }
    static void failedLogin() {
        ExecutionContext c=ExecutionContext.current();c.authentication.exhausted=true;c.authentication.reason='LOGIN_TIMEOUT_OR_LOST';c.persist()
    }
    static void authenticated() {
        ExecutionContext c=ExecutionContext.current();c.authentication.authenticated=true;c.persist()
    }
    static void requireInitialLogin() {
        requireAllowance()
        if (ExecutionContext.current().authentication.authenticated) lost('Login redirect after an authenticated page')
    }
    static void lost(String phase) {
        failedLogin()
        throw new IllegalStateException('[ENVIRONMENT / AUTHENTICATION BLOCKED] '+phase+'; submitted outcome may be uncertain. No submit replay. Only this execution is stopped.')
    }
}
