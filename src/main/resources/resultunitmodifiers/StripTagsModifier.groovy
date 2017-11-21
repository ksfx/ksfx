package resultunitmodifiers

import ch.ksfx.model.spidering.ResultUnit
import ch.ksfx.model.spidering.ResultUnitModifier
import ch.ksfx.web.services.logger.SystemLogger

class StripTagsModifier implements ResultUnitModifier
{
    private SystemLogger systemLogger;

    public DemoResultUnitModifier(SystemLogger systemLogger)
    {
        this.systemLogger = systemLogger;
    }

    public ResultUnit modify(ResultUnit resultUnit)
    {
        if (resultUnit.getValue() != null) {
            resultUnit.setValue(resultUnit.getValue().replaceAll("<.+?>", ""));
        }

        return resultUnit;
    }
}