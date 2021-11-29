package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE;
import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.MEDHOLD_I_KLAGE;
import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.OPPHEVE_YTELSESVEDTAK;

import java.util.Arrays;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.konfig.Environment;
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

    static boolean erAnkeBehandling(BehandlingVedtak behandlingVedtak) {
        return VedtakResultatType.VEDTAK_I_ANKEBEHANDLING.equals(behandlingVedtak.getVedtakResultatType());
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
                                                           KlageRepository klageRepository,
                                                           AnkeRepository ankeRepository) {
        DokumentMalType dokumentMal = null;

        if (erLagetFritekstBrev(behandlingsresultat)) {
            dokumentMal = Environment.current().isProd() ? DokumentMalType.FRITEKSTBREV_DOK : DokumentMalType.FRITEKSTBREV;
        } else if (erRevurderingMedUendretUtfall(behandlingVedtak)) {
            dokumentMal = DokumentMalType.INGEN_ENDRING;
        } else if (erKlageBehandling(behandlingVedtak)) {
            dokumentMal = velgKlagemal(behandling, klageRepository);
        } else if (erAnkeBehandling(behandlingVedtak)) {
            dokumentMal = velgAnkemal(behandling, ankeRepository);
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
        }
        return null;
    }

    public static DokumentMalType velgPositivtVedtaksmal(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        var ytelse = behandling.getFagsakYtelseType();

        return FagsakYtelseType.FORELDREPENGER.equals(ytelse) ?
            velgForeldrepengerPositivtVedtaksmal(behandlingsresultat) : FagsakYtelseType.ENGANGSTØNAD.equals(ytelse) ?
            DokumentMalType.ENGANGSSTØNAD_INNVILGELSE : FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelse) ?
            Environment.current().isProd() ? DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE_FRITEKST : DokumentMalType.SVANGERSKAPSPENGER_INNVILGELSE : null;
    }

    private static DokumentMalType velgForeldrepengerPositivtVedtaksmal(Behandlingsresultat behandlingsresultat) {
        return BehandlingResultatType.FORELDREPENGER_SENERE.equals(behandlingsresultat.getBehandlingResultatType())
            ? DokumentMalType.FORELDREPENGER_ANNULLERT : DokumentMalType.FORELDREPENGER_INNVILGELSE;
    }

    public static DokumentMalType velgKlagemal(Behandling behandling, KlageRepository klageRepository) {
        var klagevurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling).orElse(null);
        if (klagevurderingResultat == null) {
            return null;
        }
        var klagevurdering = klagevurderingResultat.getKlageVurdering();

        if (KlageVurdering.AVVIS_KLAGE.equals(klagevurdering)) {
            return DokumentMalType.KLAGE_AVVIST;
        }
        if (Arrays.asList(OPPHEVE_YTELSESVEDTAK, HJEMSENDE_UTEN_Å_OPPHEVE).contains(klagevurdering)) {
            return DokumentMalType.KLAGE_HJEMSENDT;
        }
        if (MEDHOLD_I_KLAGE.equals(klagevurdering)) {
            return DokumentMalType.KLAGE_OMGJORT;
        }
        if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(klagevurdering)) {
            return DokumentMalType.KLAGE_STADFESTET;
        }

        return null;
    }

    public static DokumentMalType velgAnkemal(Behandling behandling, AnkeRepository ankeRepository) {
        var ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId()).orElse(null);
        if (ankeVurderingResultat == null) {
            return null;
        }

        var ankeVurdering = ankeVurderingResultat.getAnkeVurdering();

        if (AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE.equals(ankeVurdering) || AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV.equals(ankeVurdering)) {
            return DokumentMalType.ANKE_BESLUTNING_OM_OPPHEVING_FRITEKST;
        }
        if (AnkeVurdering.ANKE_OMGJOER.equals(ankeVurdering)){
            return DokumentMalType.ANKE_VEDTAK_OMGJORING_FRITEKST;
        }

        return null;
    }
}
