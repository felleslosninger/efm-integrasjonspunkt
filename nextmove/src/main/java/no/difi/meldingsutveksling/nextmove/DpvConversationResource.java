package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.xml.LocalDateTimeAdapter;

import javax.persistence.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@DiscriminatorValue("DPV")
@Data
public class DpvConversationResource extends ConversationResource {

    private boolean mandatoryNotification;
    private String serviceEdition;
    private String messageTitle;
    private String messageSummary;
    private String messageBody;
    private String languageCode;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime visibleDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime allowSystemDeleteDateTime;
    private boolean allowForwarding;
    private Notifications notifications;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "conversation_id")
    private List<FileAttachement> files;

    DpvConversationResource() {}

    private DpvConversationResource(String conversationId, Sender sender, Receiver receiver) {
        super(conversationId, sender, receiver, ServiceIdentifier.DPV, LocalDateTime.now(), Maps.newHashMap(), Maps.newHashMap());
    }

    public static DpvConversationResource of(String conversationId, Sender sender, Receiver receiver) {
        return new DpvConversationResource(conversationId, sender, receiver);
    }

    public static DpvConversationResource of(DpiConversationResource dpi, IntegrasjonspunktProperties props) {
        DpvConversationResource dpv = of(dpi.getConversationId(), dpi.getSender(), dpi.getReceiver());

        dpv.setSender(dpi.getSender());
        dpv.setReceiver(dpi.getReceiver());
        dpv.setHasArkivmelding(dpi.isHasArkivmelding());
        dpv.setFileRefs(dpi.getFileRefs());
        dpv.setCustomProperties(dpi.getCustomProperties());
        dpv.setArkivmelding(dpi.getArkivmelding());

        dpv.setServiceEdition("10");
        if (!Strings.isNullOrEmpty(props.getDpv().getExternalServiceEditionCode())) {
            dpv.setServiceEdition(props.getDpv().getExternalServiceEditionCode());
        }
        dpv.setLanguageCode("1044");

        dpv.setMessageTitle(dpi.getDigitalPostInfo().getIkkeSensitivTittel());
        dpv.setMessageSummary(dpi.getDigitalPostInfo().getIkkeSensitivTittel());
        dpv.setMessageBody(dpi.getDigitalPostInfo().getIkkeSensitivTittel());

        dpv.setVisibleDateTime(LocalDateTime.now());
        dpv.setAllowSystemDeleteDateTime(LocalDateTime.now().plusMinutes(5));
        dpv.setFiles(dpi.getFiles());

        return dpv;
    }
}
