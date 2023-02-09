package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;

final class OverføringDokumentasjonUtleder {

    private OverføringDokumentasjonUtleder() {
    }

    public static Optional<DokumentasjonVurderingBehov.Behov> utledBehov(OppgittPeriodeEntitet oppgittPeriode) {
        if (!oppgittPeriode.isOverføring()) {
            return Optional.empty();
        }
        var overføringÅrsak = (OverføringÅrsak) oppgittPeriode.getÅrsak();
        var behovÅrsak = switch (overføringÅrsak) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> DokumentasjonVurderingBehov.Behov.OverføringÅrsak.INNLEGGELSE_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> DokumentasjonVurderingBehov.Behov.OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> DokumentasjonVurderingBehov.Behov.OverføringÅrsak.BARE_SØKER_RETT;
            case ALENEOMSORG -> DokumentasjonVurderingBehov.Behov.OverføringÅrsak.ALENEOMSORG;
            case UDEFINERT -> throw new IllegalArgumentException("Udefinert overføringsårsak " + overføringÅrsak);
        };
        return Optional.of(new DokumentasjonVurderingBehov.Behov(DokumentasjonVurderingBehov.Behov.Type.OVERFØRING, behovÅrsak));
    }
}
