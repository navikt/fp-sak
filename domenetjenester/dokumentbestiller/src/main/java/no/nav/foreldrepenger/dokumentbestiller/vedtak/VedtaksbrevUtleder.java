package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.exception.TekniskException;

public class VedtaksbrevUtleder {

    private VedtaksbrevUtleder() {
    }

    static boolean erAvlåttEllerOpphørt(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.AVSLAG.equals(behandlingVedtak.getVedtakResultatType())
            || VedtakResultatType.OPPHØR.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erKlageBehandling(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erInnvilget(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.INNVILGET.equals(behandlingVedtak.getVedtakResultatType());
    }

    static boolean erLagetFritekstBrev(Behandlingsresultat behandlingsresultat) {
        return Vedtaksbrev.FRITEKST.equals(behandlingsresultat.getVedtaksbrev());
    }

    public static DokumentMalType velgDokumentMalForVedtak(Behandling behandling,
                                                           Behandlingsresultat behandlingsresultat,
                                                           BehandlingVedtak behandlingVedtak,
                                                           KlageRepository klageRepository) {
        DokumentMalType dokumentMal = null;

        if (erLagetFritekstBrev(behandlingsresultat)) {
            dokumentMal = DokumentMalType.FRITEKSTBREV;
        } else if (erRevurderingMedUendretUtfall(behandlingVedtak)) {
            dokumentMal = DokumentMalType.INGEN_ENDRING;
        } else if (erKlageBehandling(behandlingVedtak)) {
            dokumentMal = velgKlagemal(behandling, klageRepository);
        } else if (erInnvilget(behandlingVedtak)) {
            dokumentMal = velgPositivtVedtaksmal(behandling, behandlingsresultat);
        } else if (erAvlåttEllerOpphørt(behandlingVedtak)) {
            dokumentMal = velgNegativVedtaksmal(behandling, behandlingsresultat);
        }
        if (dokumentMal == null) {
            throw new TekniskException("FP-666915", "Ingen brevmal konfigurert for behandling " + behandling.getId());
        }
        return dokumentMal;
    }

    static boolean erRevurderingMedUendretUtfall(BehandlingVedtak vedtak) {
        return vedtak.isBeslutningsvedtak();
    }


    public static DokumentMalType velgNegativVedtaksmal(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var fagsakYtelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)) {
            return DokumentMalType.ENGANGSSTØNAD_AVSLAG;
        }
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            if (behandlingsresultat.isBehandlingsresultatOpphørt()) {
                return DokumentMalType.FORELDREPENGER_OPPHØR;
            }
            return DokumentMalType.FORELDREPENGER_AVSLAG;
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType)) {
            if (behandlingsresultat.isBehandlingsresultatOpphørt()) {
                return DokumentMalType.SVANGERSKAPSPENGER_OPPHØR;
            }
            return DokumentMalType.SVANGERSKAPSPENGER_AVSLAG;
        }
        return null;
    }

    public static DokumentMalType velgPositivtVedtaksmal(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var ytelse = behandling.getFagsakYtelseType();

        return FagsakYtelseType.FORELDREPENGER.equals(ytelse) ?
            velgForeldrepengerPositivtVedtaksmal(behandlingsresultat) : FagsakYtelseType.ENGANGSTØNAD.equals(ytelse) ?
            DokumentMalType.ENGANGSSTØNAD_INNVILGELSE : FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelse) ?
            DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE : null;
    }

    private static DokumentMalType velgForeldrepengerPositivtVedtaksmal(Behandlingsresultat behandlingsresultat) {
        return BehandlingResultatType.FORELDREPENGER_SENERE.equals(behandlingsresultat.getBehandlingResultatType())
            ? DokumentMalType.FORELDREPENGER_ANNULLERT : DokumentMalType.FORELDREPENGER_INNVILGELSE;
    }

    public static DokumentMalType velgKlagemal(Behandling behandling, KlageRepository klageRepository) {
        var klageVurdering = klageRepository.hentGjeldendeKlageVurderingResultat(behandling).map(KlageVurderingResultat::getKlageVurdering).orElse(null);
        if (klageVurdering == null) {
            return null;
        }

        return switch (klageVurdering) {
            case MEDHOLD_I_KLAGE -> DokumentMalType.KLAGE_OMGJORT;
            case AVVIS_KLAGE -> DokumentMalType.KLAGE_AVVIST;
            case UDEFINERT, HJEMSENDE_UTEN_Å_OPPHEVE, OPPHEVE_YTELSESVEDTAK, STADFESTE_YTELSESVEDTAK -> null;
        };
    }
}
