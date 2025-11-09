package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
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
                                                           KlageVurdering klageVurdering) {

        if (erRevurderingMedUendretUtfall) {
            return DokumentMalType.INGEN_ENDRING;
        }

        var vedtakType = switch (vedtakResultatType) {
            case VEDTAK_I_KLAGEBEHANDLING -> VedtakType.KLAGE;
            case INNVILGET -> VedtakType.POSITIV;
            case AVSLAG, OPPHØR -> VedtakType.NEGATIV;
            case null, default -> null;
        };

        return velgDokumentMal(behandling, behandlingResultatType, vedtakType, klageVurdering);
    }

    public static DokumentMalType velgDokumentMalForForhåndsvisningAvVedtak(Behandling behandling,
                                                                            BehandlingResultatType behandlingResultatType,
                                                                            List<KonsekvensForYtelsen> konsekvensForYtelsenList,
                                                                            Boolean erRevurderingMedUendretUtfall,
                                                                            KlageVurdering klageVurdering) {
        if (erRevurderingMedUendretUtfall) {
            return DokumentMalType.INGEN_ENDRING;
        }

        VedtakType vedtakType = null;

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            vedtakType = VedtakType.KLAGE;
        } else if (erInnvilgetEndretEllerSenere(behandlingResultatType, konsekvensForYtelsenList)) {
            vedtakType = VedtakType.POSITIV;
        } else if (erAvslåttEllerOpphørt(behandlingResultatType)) {
            vedtakType = VedtakType.NEGATIV;
        }

        return velgDokumentMal(behandling, behandlingResultatType, vedtakType, klageVurdering);
    }

    /**
     * Denne metoden tar ikke hensyn til fritekstmalen.
     */
    private static DokumentMalType velgDokumentMal(Behandling behandling,
                                                   BehandlingResultatType behandlingResultatType,
                                                   VedtakType vedtakType,
                                                   KlageVurdering klageVurdering) {
        return switch (vedtakType) {
            case KLAGE -> velgKlagemal(klageVurdering);
            case POSITIV -> velgPositivtVedtaksmal(behandling.getFagsakYtelseType(), behandlingResultatType);
            case NEGATIV -> velgNegativVedtaksmal(behandling.getFagsakYtelseType(), behandlingResultatType);
            case null -> throw new TekniskException("FP-666915", "Ingen brevmal konfigurert for behandling " + behandling.getId());
        };
    }

    private static boolean erInnvilgetEndretEllerSenere(BehandlingResultatType resultatType, List<KonsekvensForYtelsen> konsekvensForYtelsenList) {
        return Set.of(BehandlingResultatType.INNVILGET, BehandlingResultatType.FORELDREPENGER_SENERE).contains(resultatType) || (
            BehandlingResultatType.FORELDREPENGER_ENDRET.equals(resultatType) && !erKunEndringIFordelingAvYtelsen(konsekvensForYtelsenList));
    }

    private static boolean erKunEndringIFordelingAvYtelsen(List<KonsekvensForYtelsen> konsekvensForYtelsen) {
        return konsekvensForYtelsen.contains(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN) && konsekvensForYtelsen.size() == 1;
    }

    private static boolean erAvslåttEllerOpphørt(BehandlingResultatType behandlingResultatType) {
        return Set.of(BehandlingResultatType.OPPHØR, BehandlingResultatType.AVSLÅTT).contains(behandlingResultatType);
    }

    private static DokumentMalType velgNegativVedtaksmal(FagsakYtelseType ytelseType, BehandlingResultatType behandlingResultatType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> DokumentMalType.ENGANGSSTØNAD_AVSLAG;
            case FORELDREPENGER -> erOpphør(behandlingResultatType) ? DokumentMalType.FORELDREPENGER_OPPHØR : DokumentMalType.FORELDREPENGER_AVSLAG;
            case SVANGERSKAPSPENGER ->
                erOpphør(behandlingResultatType) ? DokumentMalType.SVANGERSKAPSPENGER_OPPHØR : DokumentMalType.SVANGERSKAPSPENGER_AVSLAG;
            case null, default -> throw new TekniskException("FP-666917", "Ytelse type kan ikke være null.");
        };
    }

    private static boolean erOpphør(BehandlingResultatType behandlingResultatType) {
        return BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    private static DokumentMalType velgPositivtVedtaksmal(FagsakYtelseType ytelseType, BehandlingResultatType behandlingResultatType) {
        return switch (ytelseType) {
            case FORELDREPENGER ->
                erUtsettelse(behandlingResultatType) ? DokumentMalType.FORELDREPENGER_ANNULLERT : DokumentMalType.FORELDREPENGER_INNVILGELSE;
            case SVANGERSKAPSPENGER -> DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE;
            case ENGANGSTØNAD -> DokumentMalType.ENGANGSSTØNAD_INNVILGELSE;
            case null, default -> throw new TekniskException("FP-666918", "Ytelse type kan ikke være null.");
        };
    }

    private static boolean erUtsettelse(BehandlingResultatType behandlingResultatType) {
        return BehandlingResultatType.FORELDREPENGER_SENERE.equals(behandlingResultatType);
    }

    private static DokumentMalType velgKlagemal(KlageVurdering klageVurdering) {
        return switch (klageVurdering) {
            case MEDHOLD_I_KLAGE -> DokumentMalType.KLAGE_OMGJORT;
            case AVVIS_KLAGE -> DokumentMalType.KLAGE_AVVIST;
            case UDEFINERT, HJEMSENDE_UTEN_Å_OPPHEVE, OPPHEVE_YTELSESVEDTAK, STADFESTE_YTELSESVEDTAK ->
                throw new TekniskException("FP-666919", String.format("Klage vurdering %s skal ikke sende brev fra VL.", klageVurdering));
            case null -> throw new TekniskException("FP-666920", "Klage vurdering bør ikke være null.");
        };
    }
}
