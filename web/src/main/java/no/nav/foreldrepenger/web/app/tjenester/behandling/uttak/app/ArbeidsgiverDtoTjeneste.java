package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverDto;

@ApplicationScoped
public class ArbeidsgiverDtoTjeneste {
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    public ArbeidsgiverDtoTjeneste() {
        //For CDI
    }

    @Inject
    public ArbeidsgiverDtoTjeneste(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public ArbeidsgiverDto mapFra(Arbeidsgiver arbeidsgiver, List<ArbeidsforholdOverstyring> overstyringer) {
        if (arbeidsgiver == null) {
            return null;
        }
        if (arbeidsgiver.getErVirksomhet() && !Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            var virksomhet = arbeidsgiverTjeneste.hentVirksomhet(arbeidsgiver.getOrgnr());
            return ArbeidsgiverDto.virksomhet(arbeidsgiver.getIdentifikator(), virksomhet.getNavn());
        } else if (arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr())) {
            ArbeidsgiverOpplysninger opplysninger = FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer)
                .orElseThrow(() -> new IllegalStateException("Fant ikke forventet informasjon om manuelt arbeidsforhold"));
            return ArbeidsgiverDto.virksomhet(opplysninger.getIdentifikator(), opplysninger.getNavn());
        }
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        return ArbeidsgiverDto.person(opplysninger.getNavn(), arbeidsgiver.getAktørId(), opplysninger.getFødselsdato());
    }
}
