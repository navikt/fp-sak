package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = KlageFormkravAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class KlageFormkravOppdaterer implements AksjonspunktOppdaterer<KlageFormkravAksjonspunktDto> {

    private KlageVurderingTjeneste klageVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private KlageHistorikkinnslag klageFormkravHistorikk;

    KlageFormkravOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public KlageFormkravOppdaterer(KlageVurderingTjeneste klageVurderingTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   KlageHistorikkinnslag klageFormkravHistorikk) {
        this.behandlingRepository = behandlingRepository;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.klageFormkravHistorikk = klageFormkravHistorikk;
    }

    @Override
    public OppdateringResultat oppdater(KlageFormkravAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var apDefFormkrav = dto.getAksjonspunktDefinisjon();
        var klageVurdertAv = getKlageVurdertAv(apDefFormkrav);

        var klageBehandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var klageResultat = klageVurderingTjeneste.hentEvtOpprettKlageResultat(klageBehandling);

        if (KlageVurdertAv.NK.equals(klageVurdertAv)) {
            throw new IllegalArgumentException("KA Formkrav skal ikke lenger forekomme");
        }

        var klageFormkrav = klageVurderingTjeneste.hentKlageFormkrav(klageBehandling, klageVurdertAv);

        klageFormkravHistorikk.opprettHistorikkinnslagFormkrav(klageBehandling, apDefFormkrav, dto, klageFormkrav, klageResultat, dto.getBegrunnelse());
        var optionalAvvistÅrsak = vurderOgLagreFormkrav(dto, klageBehandling, klageResultat, klageVurdertAv);
        if (optionalAvvistÅrsak.isPresent()) {
            lagreKlageVurderingResultatMedAvvistKlage(klageBehandling, klageVurdertAv, dto.fritekstTilBrev() != null ? dto.fritekstTilBrev() : null);
            return OppdateringResultat.medFremoverHoppTotrinn(AksjonspunktOppdateringTransisjon.FORESLÅ_VEDTAK);
        }
        //Må fjerne fritekst om det ble lagret i formkrav-vurderingen
        klageVurderingTjeneste.hentKlageVurderingResultat(klageBehandling, klageVurdertAv).ifPresent(klageVurderingResultat -> {
            if (klageVurderingResultat.getFritekstTilBrev() != null) {
                var vurderingResultatBuilder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(klageBehandling, klageVurdertAv).medFritekstTilBrev(null);
                klageVurderingTjeneste.lagreKlageVurderingResultat(klageBehandling, vurderingResultatBuilder, klageVurdertAv);
            }
        });
        klageBehandling.getÅpentAksjonspunktMedDefinisjonOptional(apDefFormkrav)
            .filter(Aksjonspunkt::isToTrinnsBehandling)
            .ifPresent(ap -> fjernToTrinnsbehandling(klageBehandling, ap));

        return OppdateringResultat.utenTransisjon().build();
    }

    private void fjernToTrinnsbehandling(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.setAksjonspunktToTrinn(kontekst, aksjonspunkt, false);
    }

    private Optional<KlageAvvistÅrsak> vurderOgLagreFormkrav(KlageFormkravAksjonspunktDto dto,
                                                             Behandling behandling,
                                                             KlageResultatEntitet klageResultat,
                                                             KlageVurdertAv vurdertAv) {
        if (dto.erTilbakekreving()) {
            klageVurderingTjeneste.oppdaterKlageMedPåklagetEksternBehandlingUuid(behandling.getId(),
                dto.klageTilbakekreving().tilbakekrevingUuid());
            BehandlingÅrsak.builder(BehandlingÅrsakType.KLAGE_TILBAKEBETALING).buildFor(behandling);
        } else {
            var påKlagdBehandlingUuid = dto.påKlagdBehandlingUuid();
            if (påKlagdBehandlingUuid != null || dto.hentpåKlagdEksternBehandlingUuId() == null
                && klageResultat.getPåKlagdBehandlingId().isPresent()) {
                klageVurderingTjeneste.oppdaterKlageMedPåklagetBehandling(behandling, påKlagdBehandlingUuid);
            }
        }

        var builder = klageVurderingTjeneste.hentKlageFormkravBuilder(behandling, vurdertAv)
            .medErKlagerPart(dto.erKlagerPart())
            .medErFristOverholdt(dto.erFristOverholdt())
            .medErKonkret(dto.erKonkret())
            .medErSignert(dto.erSignert())
            .medErFristOverholdt(dto.erFristOverholdt())
            .medBegrunnelse(dto.getBegrunnelse())
            .medGjelderVedtak(dto.påKlagdBehandlingUuid() != null)
            .medKlageResultat(klageResultat);

        klageVurderingTjeneste.lagreFormkrav(behandling, builder);
        return utledAvvistÅrsak(dto, dto.påKlagdBehandlingUuid() != null);

    }

    private Optional<KlageAvvistÅrsak> utledAvvistÅrsak(KlageFormkravAksjonspunktDto dto, boolean gjelderVedtak) {
        if (!gjelderVedtak) {
            return Optional.of(KlageAvvistÅrsak.IKKE_PAKLAGD_VEDTAK);
        }
        if (!dto.erKlagerPart()) {
            return Optional.of(KlageAvvistÅrsak.KLAGER_IKKE_PART);
        }
        if (!dto.erFristOverholdt()) {
            return Optional.of(KlageAvvistÅrsak.KLAGET_FOR_SENT);
        }
        if (!dto.erKonkret()) {
            return Optional.of(KlageAvvistÅrsak.IKKE_KONKRET);
        }
        if (!dto.erSignert()) {
            return Optional.of(KlageAvvistÅrsak.IKKE_SIGNERT);
        }
        return Optional.empty();
    }

    private void lagreKlageVurderingResultatMedAvvistKlage(Behandling klageBehandling, KlageVurdertAv vurdertAv, String fritekstTilBrev) {
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(klageBehandling, vurdertAv)
            .medKlageVurdering(KlageVurdering.AVVIS_KLAGE)
            .medFritekstTilBrev(fritekstTilBrev);
        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(klageBehandling, builder, vurdertAv);
    }

    private KlageVurdertAv getKlageVurdertAv(AksjonspunktDefinisjon apdef) {
        return VURDERING_AV_FORMKRAV_KLAGE_NFP.equals(apdef) ? KlageVurdertAv.NFP : KlageVurdertAv.NK;
    }
}
