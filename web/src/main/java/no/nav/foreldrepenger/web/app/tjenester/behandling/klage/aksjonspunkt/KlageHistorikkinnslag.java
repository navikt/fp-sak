package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.format;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;

@ApplicationScoped
public class KlageHistorikkinnslag {

    private static final String IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST = "Ikke påklagd et vedtak";
    public static final String PÅKLAGD_BEHANDLING = "Påklagd behandling";

    private Historikkinnslag2Repository historikkinnslag2Repository;
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    private FptilbakeRestKlient fptilbakeRestKlient;

    KlageHistorikkinnslag() {
        // for CDI proxy
    }

    @Inject
    public KlageHistorikkinnslag(Historikkinnslag2Repository historikkinnslag2Repository,
                                 BehandlingRepository behandlingRepository,
                                 BehandlingVedtakRepository behandlingVedtakRepository,
                                 FptilbakeRestKlient fptilbakeRestKlient) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.fptilbakeRestKlient = fptilbakeRestKlient;
    }

    public void opprettHistorikkinnslagFormkrav(Behandling klageBehandling,
                                                AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                KlageFormKravLagreDto formkravDto,
                                                Optional<KlageFormkravEntitet> klageFormkrav,
                                                KlageResultatEntitet klageResultat,
                                                String begrunnelse) {
        var skjermlenkeType = getSkjermlenkeType(aksjonspunktDefinisjon);

        var linjer = klageFormkrav.map(klageFormkravEntitet -> lagHistorikkTekstlinjer(klageFormkravEntitet, formkravDto, klageResultat))
            .orElseGet(() -> lagHistorikkTekstlinjer(formkravDto));
        var historikkinnslag = new Historikkinnslag2.Builder().medTittel(skjermlenkeType)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(klageBehandling.getFagsakId())
            .medBehandlingId(klageBehandling.getId())
            .medTekstlinjerString(linjer)
            .addTekstlinje(begrunnelse)
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagVurdering(Behandling behandling,
                                                 AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                                 KlageVurderingLagreDto dto,
                                                 String begrunnelse) {
        var klageVurdering = dto.getKlageVurdering();
        var klageVurderingOmgjør = dto.getKlageVurderingOmgjoer() != null ? dto.getKlageVurderingOmgjoer() : null;
        var erNfpAksjonspunkt = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP.equals(aksjonspunktDefinisjon);

        var resultat = KlageVurderingTjeneste.historikkResultatForKlageVurdering(klageVurdering,
            erNfpAksjonspunkt ? KlageVurdertAv.NFP : KlageVurdertAv.NK, klageVurderingOmgjør);
        var tekstlinjer = new ArrayList<String>();
        if (erNfpAksjonspunkt && resultat != null) {
            tekstlinjer.add(tekstlinjeBuilder().til("Resultat", resultat.getNavn()).build());
        }
        var årsak = dto.getKlageMedholdArsak();
        if (årsak != null) {
            tekstlinjer.add(tekstlinjeBuilder().til("Årsak til omgjøring", årsak.getNavn()).build());
        }

        var skjermlenkeType = erNfpAksjonspunkt ? SkjermlenkeType.KLAGE_BEH_NFP : SkjermlenkeType.KLAGE_BEH_NK;
        var historikkinnslag = new Historikkinnslag2.Builder().medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(skjermlenkeType)
            .medTekstlinjerString(tekstlinjer)
            .addTekstlinje(begrunnelse)
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    private String hentPåklagdBehandlingTekst(Long behandlingId) {
        if (behandlingId == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        var påKlagdBehandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtaksDatoPåklagdBehandling = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(påKlagdBehandling.getId())
            .map(BehandlingVedtak::getVedtaksdato);
        return påKlagdBehandling.getType().getNavn() + " " + vedtaksDatoPåklagdBehandling.map(dato -> format(dato)).orElse("");
    }

    private String hentPåklagdBehandlingTekst(UUID behandlingUuid) {
        return hentPåklagdBehandlingTekst(behandlingUuid == null ? null : behandlingRepository.hentBehandling(behandlingUuid).getId());
    }

    private String hentPåKlagdEksternBehandlingTekst(UUID påKlagdEksternBehandlingUuid, String behandlingType, LocalDate vedtakDato) {
        if (påKlagdEksternBehandlingUuid == null) {
            return IKKE_PÅKLAGD_ET_VEDTAK_HISTORIKKINNSLAG_TEKST;
        }
        return BehandlingType.fraKode(behandlingType).getNavn() + " " + format(vedtakDato);
    }

    private List<String> lagHistorikkTekstlinjer(KlageFormKravLagreDto formkravDto) {
        var behandlingId = formkravDto.påKlagdBehandlingUuid();
        var påKlagdBehandling = !formkravDto.erTilbakekreving() ? hentPåklagdBehandlingTekst(behandlingId) : hentPåKlagdEksternBehandlingTekst(
            formkravDto.hentpåKlagdEksternBehandlingUuId(), formkravDto.klageTilbakekreving().tilbakekrevingBehandlingType(),
            formkravDto.klageTilbakekreving().tilbakekrevingVedtakDato());

        return List.of(tekstlinjeBuilder().til(PÅKLAGD_BEHANDLING, påKlagdBehandling).build(),
            tekstlinjeBuilder().til("Er klager part", formkravDto.erKlagerPart()).build(),
            tekstlinjeBuilder().til("Er klagefrist overholdt", formkravDto.erFristOverholdt()).build(),
            tekstlinjeBuilder().til("Er klagen signert", formkravDto.erSignert()).build(),
            tekstlinjeBuilder().til("Er klagen konkret", formkravDto.erKonkret()).build());


    }

    private static HistorikkinnslagTekstlinjeBuilder tekstlinjeBuilder() {
        return new HistorikkinnslagTekstlinjeBuilder();
    }

    private List<String> lagHistorikkTekstlinjer(KlageFormkravEntitet klageFormkrav,
                                                 KlageFormKravLagreDto formkravDto,
                                                 KlageResultatEntitet klageResultat) {
        var tekstlinjer = new ArrayList<String>();
        if (erVedtakOppdatert(klageResultat, formkravDto)) {
            var lagretPåklagdEksternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);
            var lagretPåklagdBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
            var klageTilbakekrevingDto = formkravDto.klageTilbakekreving();
            if (lagretPåklagdEksternBehandlingUuid != null) {
                tekstlinjer.add(lagTekstlinjeHvisForrigePåklagdEksternBehandlingUuidFinnes(formkravDto, lagretPåklagdEksternBehandlingUuid,
                    klageTilbakekrevingDto));

            } else if (lagretPåklagdBehandlingId != null) {
                tekstlinjer.add(
                    lagTekstlinjeHvisForrigePåklagdBehandlingFinnes(formkravDto, klageResultat, lagretPåklagdBehandlingId, klageTilbakekrevingDto));

            } else {
                var tilVerdi = formkravDto.erTilbakekreving() ? hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                    klageTilbakekrevingDto.tilbakekrevingBehandlingType(),
                    klageTilbakekrevingDto.tilbakekrevingVedtakDato()) : hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid());
                tekstlinjer.add(new HistorikkinnslagTekstlinjeBuilder().til(PÅKLAGD_BEHANDLING, tilVerdi).build());
            }
        }
        if (klageFormkrav.erKlagerPart() != formkravDto.erKlagerPart()) {
            tekstlinjer.add(
                new HistorikkinnslagTekstlinjeBuilder().fraTil("Er klager part", klageFormkrav.erKlagerPart(), formkravDto.erKlagerPart()).build());
        }
        if (klageFormkrav.erFristOverholdt() != formkravDto.erFristOverholdt()) {
            tekstlinjer.add(new HistorikkinnslagTekstlinjeBuilder().fraTil("Er klagefrist overholdt", klageFormkrav.erFristOverholdt(),
                formkravDto.erFristOverholdt()).build());
        }
        if (klageFormkrav.erSignert() != formkravDto.erSignert()) {
            tekstlinjer.add(
                new HistorikkinnslagTekstlinjeBuilder().fraTil("Er klagen signert", klageFormkrav.erSignert(), formkravDto.erSignert()).build());

        }
        if (klageFormkrav.erKonkret() != formkravDto.erKonkret()) {
            tekstlinjer.add(
                new HistorikkinnslagTekstlinjeBuilder().fraTil("Er klagen konkret", klageFormkrav.erKonkret(), formkravDto.erKonkret()).build());
        }
        return tekstlinjer;
    }

    private String lagTekstlinjeHvisForrigePåklagdBehandlingFinnes(KlageFormKravLagreDto formkravDto,
                                                                   KlageResultatEntitet klageResultat,
                                                                   Long lagretPåklagdBehandlingId,
                                                                   KlageTilbakekrevingDto klageTilbakekrevingDto) {
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            var fraVerdi = hentPåklagdBehandlingTekst(lagretPåklagdBehandlingId);
            var tilVerdi = hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType(), klageTilbakekrevingDto.tilbakekrevingVedtakDato());
            return HistorikkinnslagTekstlinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi).build();
        } else {
            var fraVerdi = hentPåklagdBehandlingTekst(klageResultat.getPåKlagdBehandlingId().orElse(null));
            var tilVerdi = hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid());
            return HistorikkinnslagTekstlinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi).build();
        }
    }

    private String lagTekstlinjeHvisForrigePåklagdEksternBehandlingUuidFinnes(KlageFormKravLagreDto formkravDto,
                                                                              UUID lagretPåklagdEksternBehandlingUuid,
                                                                              KlageTilbakekrevingDto klageTilbakekrevingDto) {
        var tilbakekrevingVedtakDto = fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(lagretPåklagdEksternBehandlingUuid);
        if (formkravDto.hentpåKlagdEksternBehandlingUuId() != null) {
            var fraVerdi = hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid,
                tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(), tilbakekrevingVedtakDto.tilbakekrevingVedtakDato());
            var tilVerdi = hentPåKlagdEksternBehandlingTekst(formkravDto.hentpåKlagdEksternBehandlingUuId(),
                klageTilbakekrevingDto.tilbakekrevingBehandlingType(), klageTilbakekrevingDto.tilbakekrevingVedtakDato());
            return HistorikkinnslagTekstlinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi).build();
        } else {
            var fraVerdi = hentPåKlagdEksternBehandlingTekst(lagretPåklagdEksternBehandlingUuid,
                tilbakekrevingVedtakDto.tilbakekrevingBehandlingType(), tilbakekrevingVedtakDto.tilbakekrevingVedtakDato());
            var tilVerdi = hentPåklagdBehandlingTekst(formkravDto.påKlagdBehandlingUuid());
            return HistorikkinnslagTekstlinjeBuilder.fraTilEquals(PÅKLAGD_BEHANDLING, fraVerdi, tilVerdi).build();
        }
    }

    private SkjermlenkeType getSkjermlenkeType(AksjonspunktDefinisjon apDef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(apDef) ? SkjermlenkeType.FORMKRAV_KLAGE_NFP : SkjermlenkeType.FORMKRAV_KLAGE_KA;
    }

    private boolean erVedtakOppdatert(KlageResultatEntitet klageResultat, KlageFormKravLagreDto formkravDto) {
        var lagretBehandlingId = klageResultat.getPåKlagdBehandlingId().orElse(null);
        var lagretEkternBehandlingUuid = klageResultat.getPåKlagdEksternBehandlingUuid().orElse(null);

        var påKlagdBehandlingUuid = formkravDto.påKlagdBehandlingUuid();
        var påKlagdBehandlingId =
            formkravDto.erTilbakekreving() || påKlagdBehandlingUuid == null ? null : behandlingRepository.hentBehandling(påKlagdBehandlingUuid)
                .getId();

        return !Objects.equals(lagretBehandlingId, påKlagdBehandlingId) || !Objects.equals(lagretEkternBehandlingUuid,
            formkravDto.hentpåKlagdEksternBehandlingUuId());
    }

}
