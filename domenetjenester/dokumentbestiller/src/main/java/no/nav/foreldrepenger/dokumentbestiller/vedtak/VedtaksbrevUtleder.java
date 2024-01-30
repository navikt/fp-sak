package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.exception.TekniskException;

public class VedtaksbrevUtleder {

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
        DokumentMalType dokumentMal = null;

        if (erRevurderingMedUendretUtfall) {
            dokumentMal = DokumentMalType.INGEN_ENDRING;
        } else if (VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING.equals(vedtakResultatType)) {
            dokumentMal = velgKlagemal(behandling, klageRepository);
        } else if (VedtakResultatType.INNVILGET.equals(vedtakResultatType)) {
            dokumentMal = velgPositivtVedtaksmal(behandling, behandlingResultatType);
        } else if (erAvlåttEllerOpphørt(vedtakResultatType)) {
            dokumentMal = velgNegativVedtaksmal(behandling, behandlingResultatType);
        }
        if (dokumentMal == null) {
            throw new TekniskException("FP-666915", "Ingen brevmal konfigurert for behandling " + behandling.getId());
        }
        return dokumentMal;
    }

    static boolean erAvlåttEllerOpphørt(VedtakResultatType vedtakResultatType) {
        return Set.of(VedtakResultatType.AVSLAG, VedtakResultatType.OPPHØR).contains(vedtakResultatType);
    }

    static DokumentMalType velgNegativVedtaksmal(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        return switch (behandling.getFagsakYtelseType()) {
            case ENGANGSTØNAD -> DokumentMalType.ENGANGSSTØNAD_AVSLAG;
            case FORELDREPENGER ->
                erOpphør(behandlingResultatType) ? DokumentMalType.FORELDREPENGER_OPPHØR : DokumentMalType.FORELDREPENGER_AVSLAG;
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
            case FORELDREPENGER -> erUtsettelse(behandlingResultatType) ?
                DokumentMalType.FORELDREPENGER_ANNULLERT : DokumentMalType.FORELDREPENGER_INNVILGELSE;
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
