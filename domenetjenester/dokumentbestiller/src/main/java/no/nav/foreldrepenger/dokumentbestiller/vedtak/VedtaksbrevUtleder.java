package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.exception.TekniskException;

public class VedtaksbrevUtleder {

    enum VedtakType {
        KLAGE,
        POSITIV,
        NEGATIV,
    }

    private VedtaksbrevUtleder() {
    }

    /**
     * Denne metoden tar ikke hensyn til fritekstmalen.
     */
    public static DokumentMalType velgDokumentMalForVedtak(Behandling behandling,
                                                           BehandlingResultatType behandlingResultatType,
                                                           VedtakResultatType vedtakResultatType,
                                                           Boolean erRevurderingMedUendretUtfall,
                                                           KlageRepository klageRepository) {

        var vedtakType = switch (vedtakResultatType) {
            case VEDTAK_I_KLAGEBEHANDLING -> VedtakType.KLAGE;
            case INNVILGET -> VedtakType.POSITIV;
            case AVSLAG, OPPHØR -> VedtakType.NEGATIV;
            case null, default -> null;
        };

        return velgDokumentMal(behandling, behandlingResultatType, erRevurderingMedUendretUtfall, vedtakType, klageRepository);
    }

    public static DokumentMalType velgDokumentMalForForhåndsvisningAvVedtak(Behandling behandling,
                                                                            BehandlingResultatType behandlingResultatType,
                                                                            List<KonsekvensForYtelsen> konsekvensForYtelsenList,
                                                                            Boolean erRevurderingMedUendretUtfall,
                                                                            KlageRepository klageRepository) {

        var vedtakType = utledVedtakTypeUtenVedtak(behandlingResultatType, konsekvensForYtelsenList, behandling.getType());

        return velgDokumentMal(behandling, behandlingResultatType, erRevurderingMedUendretUtfall, vedtakType, klageRepository);
    }

    /**
     * Denne metoden tar ikke hensyn til fritekstmalen.
     */
    private static DokumentMalType velgDokumentMal(Behandling behandling,
                                                   BehandlingResultatType behandlingResultatType,
                                                   Boolean erRevurderingMedUendretUtfall,
                                                   VedtakType vedtakType,
                                                   KlageRepository klageRepository) {
        if (VedtakType.KLAGE.equals(vedtakType)) {
            Objects.requireNonNull(klageRepository, "klageRepository må være satt.");
        }

        if (erRevurderingMedUendretUtfall) {
            return DokumentMalType.INGEN_ENDRING;
        }

        return switch (vedtakType) {
            case KLAGE -> velgKlagemal(behandling, klageRepository);
            case POSITIV -> velgPositivtVedtaksmal(behandling, behandlingResultatType);
            case NEGATIV -> velgNegativVedtaksmal(behandling, behandlingResultatType);
            case null -> throw new TekniskException("FP-666915", "Ingen brevmal konfigurert for behandling " + behandling.getId());
        };
    }

    /**
     * Denne metoden brukes til å beregne vedtaksbrev type basert kun på behandlingsresultat hvor behandlingen er allerede
     * i foreslå vedtak, fatte vedtak steget men er ikke fattet ennå.
     * @param behandlingResultatType behandling resutlat type
     * @param konsekvensForYtelsenList Liste med konsekvens for ytelsen.
     * @param behandlingType Gjeldende BehandlingType - kun for klage.
     * @return Type av vedtaksbrev som skal fattes (Klage, Positiv, Negativ) - blir senere brukt til å utlede riktig brev mal.
     */
    private static VedtakType utledVedtakTypeUtenVedtak(BehandlingResultatType behandlingResultatType,
                                                        List<KonsekvensForYtelsen> konsekvensForYtelsenList,
                                                        BehandlingType behandlingType) {
        if (BehandlingType.KLAGE.equals(behandlingType)) {
            return VedtakType.KLAGE;
        } else if (erInnvilgetEllerEndret(behandlingResultatType, konsekvensForYtelsenList)) {
            return VedtakType.POSITIV;
        } else if (erAvslåttEllerOpphørt(behandlingResultatType)) {
            return VedtakType.NEGATIV;
        }
        return null;
    }

    private static boolean erInnvilgetEllerEndret(BehandlingResultatType resultatType, List<KonsekvensForYtelsen> konsekvensForYtelsenList) {
        return BehandlingResultatType.INNVILGET.equals(resultatType) ||
            (BehandlingResultatType.FORELDREPENGER_ENDRET.equals(resultatType) && !erKunEndringIFordelingAvYtelsen(konsekvensForYtelsenList));
    }

    private static boolean erKunEndringIFordelingAvYtelsen(List<KonsekvensForYtelsen> konsekvensForYtelsen) {
        return konsekvensForYtelsen.contains(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN) && konsekvensForYtelsen.size() == 1;
    }

    private static boolean erAvslåttEllerOpphørt(BehandlingResultatType behandlingResultatType) {
        return Set.of(BehandlingResultatType.OPPHØR, BehandlingResultatType.AVSLÅTT).contains(behandlingResultatType);
    }

    static DokumentMalType velgNegativVedtaksmal(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        return switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> DokumentMalType.ENGANGSSTØNAD_AVSLAG;
            case FORELDREPENGER -> erOpphør(behandlingResultatType) ? DokumentMalType.FORELDREPENGER_OPPHØR : DokumentMalType.FORELDREPENGER_AVSLAG;
            case SVANGERSKAPSPENGER ->
                erOpphør(behandlingResultatType) ? DokumentMalType.SVANGERSKAPSPENGER_OPPHØR : DokumentMalType.SVANGERSKAPSPENGER_AVSLAG;
            case null, default -> null;
        };
    }

    private static boolean erOpphør(BehandlingResultatType behandlingResultatType) {
        return BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    static DokumentMalType velgPositivtVedtaksmal(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        return switch (behandling.getFagsakYtelseType()) {
            case FORELDREPENGER ->
                erUtsettelse(behandlingResultatType) ? DokumentMalType.FORELDREPENGER_ANNULLERT : DokumentMalType.FORELDREPENGER_INNVILGELSE;
            case SVANGERSKAPSPENGER -> DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE;
            case ENGANGSTØNAD -> DokumentMalType.ENGANGSSTØNAD_INNVILGELSE;
            case null, default -> null;
        };
    }

    private static boolean erUtsettelse(BehandlingResultatType behandlingResultatType) {
        return BehandlingResultatType.FORELDREPENGER_SENERE.equals(behandlingResultatType);
    }

    static DokumentMalType velgKlagemal(Behandling behandling, KlageRepository klageRepository) {
        var klageVurdering = klageRepository.hentGjeldendeKlageVurderingResultat(behandling)
            .map(KlageVurderingResultat::getKlageVurdering)
            .orElse(null);

        return switch (klageVurdering) {
            case MEDHOLD_I_KLAGE -> DokumentMalType.KLAGE_OMGJORT;
            case AVVIS_KLAGE -> DokumentMalType.KLAGE_AVVIST;
            case UDEFINERT, HJEMSENDE_UTEN_Å_OPPHEVE, OPPHEVE_YTELSESVEDTAK, STADFESTE_YTELSESVEDTAK -> null;
            case null -> null;
        };
    }
}
