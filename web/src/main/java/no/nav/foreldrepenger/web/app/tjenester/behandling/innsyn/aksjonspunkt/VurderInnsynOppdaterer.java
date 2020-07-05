package no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.aksjonspunkt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.domene.vedtak.innsyn.InnsynTjeneste;
import no.nav.foreldrepenger.validering.FeltFeilDto;
import no.nav.foreldrepenger.validering.Valideringsfeil;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderInnsynDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderInnsynOppdaterer implements AksjonspunktOppdaterer<VurderInnsynDto> {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private InnsynTjeneste innsynTjeneste;

    VurderInnsynOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public VurderInnsynOppdaterer(BehandlingskontrollTjeneste behandlingskontrollTjeneste, InnsynTjeneste innsynTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.innsynTjeneste = innsynTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderInnsynDto dto, AksjonspunktOppdaterParameter param) {
        param.getVilkårResultatBuilder().medVilkårResultatType(VilkårResultatType.UDEFINERT);
        Behandling behandling = param.getBehandling();
        var builder = InnsynEntitet.InnsynBuilder.builder();
        builder
            .medBehandlingId(param.getBehandlingId())
            .medInnsynResultatType(dto.getInnsynResultatType())
            .medMottattDato(dto.getMottattDato())
            .medBegrunnelse(dto.getBegrunnelse());

        dto.getInnsynDokumenter().forEach(d -> builder.medInnsynDokument(new InnsynDokumentEntitet(d.isFikkInnsyn(), d.getJournalpostId(), d.getDokumentId())));

        innsynTjeneste.lagreVurderInnsynResultat(behandling, builder.build());

        if (dto.isSattPaVent()) {
            behandlingskontrollTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.VENT_PÅ_SCANNING,
                BehandlingStegType.VURDER_INNSYN, frist(dto.getFristDato()), Venteårsak.SCANN);
        }

        return OppdateringResultat.utenOveropp();
    }

    private static LocalDateTime frist(LocalDate frist) {
        if (frist == null) {
            throw new Valideringsfeil(Collections.singleton(new FeltFeilDto("frist", "frist må være satt")));
        }
        return LocalDateTime.of(frist, LocalDateTime.now().toLocalTime());
    }
}
