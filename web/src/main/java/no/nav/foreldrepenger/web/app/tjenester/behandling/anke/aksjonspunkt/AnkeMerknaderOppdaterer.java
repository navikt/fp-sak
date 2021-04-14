package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AnkeMerknaderResultatAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class AnkeMerknaderOppdaterer implements AksjonspunktOppdaterer<AnkeMerknaderResultatAksjonspunktDto> {

    private static final AksjonspunktDefinisjon VENT_TRYGDERETT = AksjonspunktDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN;

    private BehandlingRepository behandlingRepository;
    private AnkeVurderingTjeneste ankeVurderingTjeneste;

    AnkeMerknaderOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AnkeMerknaderOppdaterer(BehandlingRepository behandlingRepository,
                                   AnkeVurderingTjeneste ankeVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AnkeMerknaderResultatAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        håndterAnkeVurdering(behandling, dto);

        var builder = OppdateringResultat.utenTransisjon();
        if (!dto.skalAvslutteBehandling()) {
            var frist = behandling.getAksjonspunktMedDefinisjonOptional(VENT_TRYGDERETT).map(Aksjonspunkt::getFristTid)
                .orElseGet(() -> LocalDateTime.now().plus(Objects.requireNonNull(VENT_TRYGDERETT.getFristPeriod())));
            if (frist.isBefore(LocalDateTime.now().plusWeeks(4)))
                frist = LocalDateTime.now().plusWeeks(4);
            var apVent = AksjonspunktResultat.opprettForAksjonspunktMedFrist(VENT_TRYGDERETT, Venteårsak.ANKE_OVERSENDT_TIL_TRYGDERETTEN, frist);
            builder.medEkstraAksjonspunktResultat(apVent, AksjonspunktStatus.OPPRETTET);
        }
        return builder.build();
    }

    private void håndterAnkeVurdering(Behandling behandling, AnkeMerknaderResultatAksjonspunktDto dto) {
        ankeVurderingTjeneste.oppdaterBekreftetMerknaderAksjonspunkt(behandling, dto.erMerknaderMottatt(), dto.getMerknadKommentar(),
            dto.getTrygderettVurdering(), dto.getTrygderettVurderingOmgjoer(), dto.getTrygderettOmgjoerArsak());
    }

}
