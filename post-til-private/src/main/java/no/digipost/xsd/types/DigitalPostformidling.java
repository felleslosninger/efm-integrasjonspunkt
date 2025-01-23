
package no.digipost.xsd.types;

import no.difi.begrep.sdp.schema_v10.*;
import no.digipost.org.w3.xmldsig.Reference;
import no.digipost.org.w3.xmldsig.Signature;


/**
 * Felles type for meldinger som har med formidling av post å gjøre, hvor det
 * vanligste er {@link SDPDigitalPost vanlig post som sendes fra avsender til mottaker},
 * men også {@link SDPFlyttetDigitalPost flyttet post fra en postkasseleverandør til en annen}.
 */
public interface DigitalPostformidling {

	Signature getSignature();
	SDPAvsender getAvsender();
	SDPMottaker getMottaker();
	SDPDigitalPostInfo getDigitalPostInfo();

	Reference getDokumentpakkefingeravtrykk();
	void setDokumentpakkefingeravtrykk(Reference fingeravtrykk);

}
