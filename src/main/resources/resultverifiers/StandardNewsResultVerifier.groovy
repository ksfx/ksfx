import ch.ksfx.model.spidering.Result
import ch.ksfx.model.spidering.ResultUnit
import ch.ksfx.model.spidering.ResultVerifier
import org.apache.tapestry5.ioc.ObjectLocator

class StandardNewsResultVerifier implements ResultVerifier
{
    private ObjectLocator objectLocator;

    public StandardNewsResultVerifier(ObjectLocator objectLocator)
    {
        this.objectLocator = objectLocator;
    }

    public boolean isResultValid(Result result)
    {
        ResultUnit titleResultUnit = result.getResultUnitForResultUnitTypeName("title");
        ResultUnit mainTextResultUnit = result.getResultUnitForResultUnitTypeName("main text");
        ResultUnit timestampResultUnit = result.getResultUnitForResultUnitTypeName("timestamp");

        if (titleResultUnit == null || titleResultUnit.getValue() == null || titleResultUnit.getValue().size() < 5) {
            return false;
        }

        if (mainTextResultUnit == null || mainTextResultUnit.getValue() == null || mainTextResultUnit.getValue().size() < 300) {
            return false;
        }

        if (timestampResultUnit == null || timestampResultUnit.getValue() == null || timestampResultUnit.getValue().size() < 5) {
            return false;
        }

        return true;
    }
}