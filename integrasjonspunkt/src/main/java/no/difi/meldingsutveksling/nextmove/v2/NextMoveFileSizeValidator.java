package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.exceptions.MaxFileSizeExceededException;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Component
public class NextMoveFileSizeValidator {

    private final IntegrasjonspunktProperties props;

    public NextMoveFileSizeValidator(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    public void validate(NextMoveOutMessage msg, MultipartFile file) {
        long totalBytes = file.getSize();
        if (msg.getFiles() != null) {
            for (var f : msg.getFiles()) {
                totalBytes += f.getSize();
            }
        }
        DataSize total = DataSize.ofBytes(totalBytes);
        DataSize limit;
        ServiceIdentifier si = msg.getServiceIdentifier();
        switch (si) {
            case DPO:
                limit = props.getDpo().getUploadSizeLimit();
                break;
            case DPV:
                limit = props.getDpv().getUploadSizeLimit();
                break;
            case DPE:
                limit = props.getNextmove().getServiceBus().getUploadSizeLimit();
                break;
            case DPF:
                limit = props.getFiks().getUt().getUploadSizeLimit();
                break;
            case DPI:
                limit = props.getDpi().getUploadSizeLimit();
                break;
            case DPFIO:
                limit = props.getFiks().getIo().getUploadSizeLimit();
                break;
            case DPH:
                limit = props.getDph().getUploadSizeLimit();
                break;
            default:
                throw new NextMoveRuntimeException("Unknown Service Identifier");
        }
        if (total.compareTo(limit) > 0) {
            if (total.toBytes() < DataSize.ofMegabytes(1L).toBytes()) {
                throw new MaxFileSizeExceededException(total.toBytes() + "b", si.toString(), limit.toBytes() + "b");
            }
            throw new MaxFileSizeExceededException(total.toMegabytes() + "MB", si.toString(), limit.toMegabytes() + "MB");
        }
    }

}
