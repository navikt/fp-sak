package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import no.nav.vedtak.exception.TekniskException;

public class KanIkkeUtledeGjeldendeFødselsdatoException extends TekniskException {

    public KanIkkeUtledeGjeldendeFødselsdatoException(String kode, String msg) {
        super(kode, msg);
    }
}
