package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;

import java.util.Optional;

final class OverføringDokumentasjonUtleder {

    private OverføringDokumentasjonUtleder() {
    }

    public static Optional<DokumentasjonVurderingBehov.Behov> utledBehov(OppgittPeriodeEntitet oppgittPeriode) {
        if (!oppgittPeriode.isOverføring()) {
            return Optional.empty();
        }
        var overføringÅrsak = (OverføringÅrsak) oppgittPeriode.getÅrsak();
        var behovÅrsak = switch (overføringÅrsak) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> DokumentasjonVurderingBehov.Behov.Årsak.INNLEGGELSE_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> DokumentasjonVurderingBehov.Behov.Årsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> DokumentasjonVurderingBehov.Behov.Årsak.BARE_SØKER_RETT;
            case ALENEOMSORG -> DokumentasjonVurderingBehov.Behov.Årsak.ALENEOMSORG;
            case UDEFINERT -> throw new IllegalArgumentException("Udefinert overføringsårsak " + overføringÅrsak);
        };
        return Optional.of(new DokumentasjonVurderingBehov.Behov(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, behovÅrsak));
    }
}
