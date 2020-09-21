package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE;
import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.MEDHOLD_I_KLAGE;
import static no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering.OPPHEVE_YTELSESVEDTAK;

import java.util.Arrays;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.BrevFeil;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

public class VedtaksbrevUtleder {
    private static final String FPSAK_FRITEKSTBREV_FOR_INNV_ENGANGSSTONAD = "fpsak.fritekstForInnvilgelseEngangsstonad";

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

    public static DokumentMalType velgDokumentMalForVedtak(Behandling behandling, Behandlingsresultat behandlingsresultat,
                                                           BehandlingVedtak behandlingVedtak,
                                                           KlageRepository klageRepository,
                                                           AnkeRepository ankeRepository,
                                                           Unleash unleash) {
        DokumentMalType dokumentMal = null;

        if (erLagetFritekstBrev(behandlingsresultat)) {
            dokumentMal = DokumentMalType.FRITEKST_DOK;
        } else if (erRevurderingMedUendretUtfall(behandlingVedtak)) {
            dokumentMal = DokumentMalType.UENDRETUTFALL_DOK;
        } else if (erKlageBehandling(behandlingVedtak)) {
            dokumentMal = velgKlagemal(behandling, klageRepository);
        } else if (erAnkeBehandling(behandlingVedtak)) {
            dokumentMal = velgAnkemal(behandling, ankeRepository);
        } else if (erInnvilget(behandlingVedtak)) {
            dokumentMal = velgPositivtVedtaksmal(behandling, unleash);
        } else if (erAvlåttEllerOpphørt(behandlingVedtak)) {
            dokumentMal = velgNegativVedtaksmal(behandling, behandlingsresultat);
        }
        if (dokumentMal == null) {
            throw BrevFeil.FACTORY.ingenBrevmalKonfigurert(behandling.getId()).toException();
        }
        return dokumentMal;
    }

    static boolean erRevurderingMedUendretUtfall(BehandlingVedtak vedtak) {
        return vedtak.isBeslutningsvedtak();
    }

    public static DokumentMalType velgNegativVedtaksmal(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)) {
            return DokumentMalType.AVSLAGSVEDTAK_DOK;
        } else if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            if (behandlingsresultat.isBehandlingsresultatOpphørt()) {
                return DokumentMalType.OPPHØR_DOK;
            } else {
                return DokumentMalType.AVSLAG_FORELDREPENGER_DOK;
            }
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType)) {
            return null; //TODO Implementer
        }
        return null;
    }

    public static DokumentMalType velgPositivtVedtaksmal(Behandling behandling, Unleash unleash) {
        FagsakYtelseType ytelse = behandling.getFagsakYtelseType();
        DokumentMalType dokumentMalTypeES;
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelse) && (unleash != null && unleash.isEnabled(FPSAK_FRITEKSTBREV_FOR_INNV_ENGANGSSTONAD, false))) {
            dokumentMalTypeES = DokumentMalType.INNVILGELSE_ENGANGSSTØNAD;
        } else {
            dokumentMalTypeES = DokumentMalType.POSITIVT_VEDTAK_DOK;
        }

        return FagsakYtelseType.FORELDREPENGER.equals(ytelse) ?
            DokumentMalType.INNVILGELSE_FORELDREPENGER_DOK : FagsakYtelseType.ENGANGSTØNAD.equals(ytelse) ?
            dokumentMalTypeES : FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelse) ?
            DokumentMalType.INNVILGELSE_SVANGERSKAPSPENGER_DOK : null;
    }

    public static DokumentMalType velgKlagemal(Behandling behandling, KlageRepository klageRepository) {
        KlageVurderingResultat klagevurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling).orElse(null);
        if (klagevurderingResultat == null) {
            return null;
        }
        KlageVurdering klagevurdering = klagevurderingResultat.getKlageVurdering();

        if (KlageVurdering.AVVIS_KLAGE.equals(klagevurdering)) {
            return DokumentMalType.KLAGE_AVVIST;
        } else if (Arrays.asList(OPPHEVE_YTELSESVEDTAK, HJEMSENDE_UTEN_Å_OPPHEVE).contains(klagevurdering)) {
            return DokumentMalType.KLAGE_HJEMSENDT;
        } else if (MEDHOLD_I_KLAGE.equals(klagevurdering)) {
            return DokumentMalType.KLAGE_OMGJØRING;
        } else if (KlageVurdering.STADFESTE_YTELSESVEDTAK.equals(klagevurdering)) {
            return DokumentMalType.KLAGE_STADFESTET;
        }

        return null;
    }

    public static DokumentMalType velgAnkemal(Behandling behandling, AnkeRepository ankeRepository) {
        AnkeVurderingResultatEntitet ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId()).orElse(null);
        if (ankeVurderingResultat == null) {
            return null;
        }

        AnkeVurdering ankeVurdering = ankeVurderingResultat.getAnkeVurdering();

        if (AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE.equals(ankeVurdering)) {
            return DokumentMalType.ANKEBREV_BESLUTNING_OM_OPPHEVING;
        }
        else if(AnkeVurdering.ANKE_OMGJOER.equals(ankeVurdering)){
            return DokumentMalType.VEDTAK_OMGJORING_ANKE_DOK;
        }
        return null;
    }
}
