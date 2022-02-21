package no.nav.foreldrepenger.domene.risikoklassifisering.mapper;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalGruppeWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.kontrakter.risk.kodeverk.RisikoklasseType;
import no.nav.foreldrepenger.kontrakter.risk.v1.RisikovurderingResultatDto;
import no.nav.vedtak.exception.TekniskException;

public class KontrollresultatMapper {

    private KontrollresultatMapper() {
        // Skjuler default
    }

    public static no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering mapFaresignalvurderingTilKontrakt(FaresignalVurdering faresignalVurdering) {
        return switch (faresignalVurdering) {
            case INNVIRKNING -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INNVIRKNING;
            case INNVILGET_REDUSERT -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INNVILGET_REDUSERT;
            case INNVILGET_UENDRET -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INNVILGET_UENDRET;
            case AVSLAG_FARESIGNAL -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.AVSLAG_FARESIGNAL;
            case AVSLAG_ANNET -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.AVSLAG_ANNET;
            case INGEN_INNVIRKNING -> no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering.INGEN_INNVIRKNING;
            case UDEFINERT -> throw new IllegalStateException("Kode UDEFINERT er ugyldig vurdering av faresignaler");
        };
    }

    public static FaresignalVurdering mapFaresignalvurderingTilDomene(no.nav.foreldrepenger.kontrakter.risk.kodeverk.FaresignalVurdering faresignalVurdering) {
        return switch (faresignalVurdering) {
            case INNVIRKNING -> FaresignalVurdering.INNVIRKNING;
            case INNVILGET_REDUSERT -> FaresignalVurdering.INNVILGET_REDUSERT;
            case INNVILGET_UENDRET -> FaresignalVurdering.INNVILGET_UENDRET;
            case AVSLAG_FARESIGNAL -> FaresignalVurdering.AVSLAG_FARESIGNAL;
            case AVSLAG_ANNET -> FaresignalVurdering.AVSLAG_ANNET;
            case INGEN_INNVIRKNING -> FaresignalVurdering.INGEN_INNVIRKNING;
        };
    }

    public static FaresignalWrapper fraFaresignalRespons(RisikovurderingResultatDto resultatKontrakt) {
        if (resultatKontrakt.risikoklasse() == null) {
            // Her ønsker vi ikke akseptere at risikoklasse er null
            throw manglerKontrollresultatkode();
        }
        return new FaresignalWrapper(mapKontrollresultatTilDomene(resultatKontrakt.risikoklasse()),
            resultatKontrakt.faresignalvurdering() == null ? null : mapFaresignalvurderingTilDomene(resultatKontrakt.faresignalvurdering()),
            mapFaresignalgruppe(resultatKontrakt.medlemskapFaresignalerNonNull()).orElse(null),
            mapFaresignalgruppe(resultatKontrakt.opptjeningFaresignalerNonNull()).orElse(null));
    }

    private static Optional<FaresignalGruppeWrapper> mapFaresignalgruppe(List<String> faresignaler) {
        if (faresignaler.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FaresignalGruppeWrapper(faresignaler));
    }

    private static TekniskException udefinertKontrollresultat() {
        return new TekniskException("FP-42518", "Udefinert kontrollresultat");
    }

    private static TekniskException manglerKontrollresultatkode() {
        return new TekniskException("FP-42517", "Mangler kontrollresultatkode på kontrollresultat");
    }

    private static Kontrollresultat mapKontrollresultatTilDomene(RisikoklasseType kontrakt) {
        return switch (kontrakt) {
            case HØY -> Kontrollresultat.HØY;
            case IKKE_HØY -> Kontrollresultat.IKKE_HØY;
            case IKKE_KLASSIFISERT -> Kontrollresultat.IKKE_KLASSIFISERT;
        };
    }
}
