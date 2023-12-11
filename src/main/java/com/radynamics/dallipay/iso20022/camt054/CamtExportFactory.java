package com.radynamics.dallipay.iso20022.camt054;

import com.radynamics.dallipay.VersionController;
import com.radynamics.dallipay.iso20022.camt054.camt05300108.Camt05300108Writer;
import com.radynamics.dallipay.iso20022.camt054.camt05400102.Camt05400102Writer;
import com.radynamics.dallipay.iso20022.camt054.camt05400104.Camt05400104Writer;
import com.radynamics.dallipay.iso20022.camt054.camt05400109.Camt05400109Writer;
import com.radynamics.dallipay.transformation.TransformInstruction;

public class CamtExportFactory {
    public static final CamtExport create(CamtFormat format, LedgerCurrencyFormat ledgerCurrencyFormat, TransformInstruction transformInstruction, VersionController versionController) {
        var export = new CamtExport();
        if (format == Camt05300108Writer.ExportFormat) {
            export.setWriter(new Camt05300108Writer(transformInstruction.getLedger(), transformInstruction, versionController.getVersion(), ledgerCurrencyFormat));
            export.setConverter(new CamtConverter(com.radynamics.dallipay.iso20022.camt054.camt05300108.generated.Document.class));
        } else if (format == Camt05400102Writer.ExportFormat) {
            export.setWriter(new Camt05400102Writer(transformInstruction.getLedger(), transformInstruction, versionController.getVersion(), ledgerCurrencyFormat));
            export.setConverter(new CamtConverter(com.radynamics.dallipay.iso20022.camt054.camt05400102.generated.Document.class));
        } else if (format == Camt05400104Writer.ExportFormat) {
            export.setWriter(new Camt05400104Writer(transformInstruction.getLedger(), transformInstruction, versionController.getVersion(), ledgerCurrencyFormat));
            export.setConverter(new CamtConverter(com.radynamics.dallipay.iso20022.camt054.camt05400104.generated.Document.class));
        } else if (format == Camt05400109Writer.ExportFormat) {
            export.setWriter(new Camt05400109Writer(transformInstruction.getLedger(), transformInstruction, versionController.getVersion(), ledgerCurrencyFormat));
            export.setConverter(new CamtConverter(com.radynamics.dallipay.iso20022.camt054.camt05400109.generated.Document.class));
        } else {
            throw new IllegalStateException("Unexpected value: " + format);
        }
        return export;
    }
}
