package no.nav.foreldrepenger.web.app.tjenester.behandling.risikoklassifisering;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderFaresignalerDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderFaresignalerOppdaterer implements AksjonspunktOppdaterer<VurderFaresignalerDto> {

    private Historikkinnslag2Repository historikkinnslag2Repository;
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private BehandlingRepository behandlingRepository;

    VurderFaresignalerOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderFaresignalerOppdaterer(RisikovurderingTjeneste risikovurderingTjeneste,
                                        Historikkinnslag2Repository historikkinnslag2Repository,
                                        BehandlingRepository behandlingRepository) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderFaresignalerDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var referanse = BehandlingReferanse.fra(behandling);

        var risikoklassifisering = risikovurderingTjeneste.hentRisikoklassifisering(referanse)
            .filter(rk -> Kontrollresultat.HØY.equals(rk.kontrollresultat()))
            .orElseThrow(() -> new IllegalStateException("Skal ikke kunne vurdere faresignaler for behandling med id " + referanse.behandlingId()));

        var originalFaresignalVurdering = risikoklassifisering.faresignalVurdering();

        risikovurderingTjeneste.lagreVurderingAvFaresignalerForBehandling(referanse, dto.getFaresignalVurdering());
        lagreHistorikkinnslag(param, dto, originalFaresignalVurdering);

        return OppdateringResultat.utenOverhopp();
    }

    private void lagreHistorikkinnslag(AksjonspunktOppdaterParameter param,
                                       VurderFaresignalerDto dto,
                                       FaresignalVurdering originalFaresignalVurdering) {
        var fraVerdi = finnTekstForFaresignalVurdering(originalFaresignalVurdering);
        var tilVerdi = finnTekstForFaresignalVurdering(dto.getFaresignalVurdering());
        if (param.erBegrunnelseEndret() || !Objects.equals(fraVerdi, tilVerdi)) {
            var historikkinnslag = new Historikkinnslag2.Builder().medBehandlingId(param.getBehandlingId())
                .medFagsakId(param.getFagsakId())
                .medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medTittel(SkjermlenkeType.VURDER_FARESIGNALER)
                .addlinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Faresignaler", fraVerdi, tilVerdi))
                .addLinje(dto.getBegrunnelse())
                .build();
            historikkinnslag2Repository.lagre(historikkinnslag);
        }
    }

    private String finnTekstForFaresignalVurdering(FaresignalVurdering faresignalVurdering) {
        if (faresignalVurdering == null || FaresignalVurdering.UDEFINERT.equals(faresignalVurdering)) {
            return null;
        }
        return Objects.equals(faresignalVurdering, FaresignalVurdering.INGEN_INNVIRKNING) ? "Ingen innvirkning" : "Innvirkning";
    }
}
