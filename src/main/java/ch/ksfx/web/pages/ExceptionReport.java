package ch.ksfx.web.pages;

import ch.ksfx.util.StacktraceUtil;
import ch.ksfx.web.services.version.Version;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ExceptionReporter;

@Import(stylesheet = {"context:styles/main_tb.css"})
public class ExceptionReport implements ExceptionReporter
{
    @Inject
    private Version version;

    @Property
    private String message;

    @Override
    public void reportException(Throwable exception)
    {
        message = StacktraceUtil.getStackTrace(exception);
    }

    public Version getVersion()
    {
        return version;
    }
}
