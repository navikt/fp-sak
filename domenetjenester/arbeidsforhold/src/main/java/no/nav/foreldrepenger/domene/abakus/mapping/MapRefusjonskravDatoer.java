package no.nav.foreldrepenger.domene.abakus.mapping;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoerDto;

public class MapRefusjonskravDatoer {

    private MapRefusjonskravDatoer() {
        // Skjul
    }

    public static List<RefusjonskravDato> map(RefusjonskravDatoerDto dto) {
        if (dto == null) {
            return null;
        }
        return dto.getRefusjonskravDatoer().stream().map(MapRefusjonskravDatoer::mapRefusjonskravDato).collect(Collectors.toList());
    }

    private static RefusjonskravDato mapRefusjonskravDato(RefusjonskravDatoDto rd) {
        return new RefusjonskravDato(mapArbeidsgiver(rd.getArbeidsgiver()), rd.getFørsteDagMedRefusjonskrav(), rd.getFørsteInnsendingAvRefusjonskrav());
    }

    private static Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiverDto) {
        if (arbeidsgiverDto == null) {
            return null;
        }
        String identifikator = arbeidsgiverDto.getIdent();
        if (arbeidsgiverDto.getErOrganisasjon()) {
            return Arbeidsgiver.virksomhet(new OrgNummer(identifikator));
        }
        if (arbeidsgiverDto.getErPerson()) {
            return Arbeidsgiver.person(new AktørId(identifikator));
        }
        throw new IllegalArgumentException();
    }

}
