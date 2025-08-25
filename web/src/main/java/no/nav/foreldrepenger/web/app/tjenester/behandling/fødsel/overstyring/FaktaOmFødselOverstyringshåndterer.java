package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.overstyring;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.historikk.FødselHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.FaktaFødselTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaOmFødselDto.class, adapter = Overstyringshåndterer.class)
public class FaktaOmFødselOverstyringshåndterer implements Overstyringshåndterer<OverstyringFaktaOmFødselDto> {
    private HistorikkinnslagRepository historikkRepository;
    private FaktaFødselTjeneste faktaFødselTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;


    FaktaOmFødselOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FaktaOmFødselOverstyringshåndterer(HistorikkinnslagRepository historikkRepository,
                                              FaktaFødselTjeneste faktaFødselTjeneste,
                                              FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.historikkRepository = historikkRepository;
        this.faktaFødselTjeneste = faktaFødselTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaOmFødselDto dto, BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);

        faktaFødselTjeneste.overstyrFaktaOmFødsel(behandlingId, dto);
        opprettHistorikkinnslag(ref, dto, familieHendelse);
        return OppdateringResultat.utenOverhopp();
    }

    private void opprettHistorikkinnslag(BehandlingReferanse ref, OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        var historikkinnslag = new Historikkinnslag.Builder().medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .addLinje(new HistorikkinnslagLinjeBuilder().bold("Overstyrt fakta om fødsel"));

        var originalFinnesFødteBarn = Optional.of(familieHendelse.getOverstyrtVersjon().filter(o -> !o.getBarna().isEmpty()).isPresent()).orElse(null);
        var dtoFinnesFødteBarn = !dto.getBarn().isEmpty();
        if (!Objects.equals(dtoFinnesFødteBarn, originalFinnesFødteBarn)) {
            historikkinnslag.addLinje(new HistorikkinnslagLinjeBuilder().fraTil("Er barnet født?", originalFinnesFødteBarn, dtoFinnesFødteBarn));
        }

        if (dtoFinnesFødteBarn) {
            FødselHistorikkTjeneste.lagHistorikkForBarn(historikkinnslag, familieHendelse, dto.getBarn());
        }

        var gjeldendeTerminDato = familieHendelse.getGjeldendeVersjon().getTermindato().orElse(null);
        if (dto.getTermindato() != null && !dto.getTermindato().equals(gjeldendeTerminDato)) {
            historikkinnslag.addLinje(lagTermindatoLinje(dto, familieHendelse));
        }

        historikkinnslag.addLinje(dto.getBegrunnelse());
        historikkRepository.lagre(historikkinnslag.build());
    }

    private static HistorikkinnslagLinjeBuilder lagTermindatoLinje(OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        return fraTilEquals("Termindato", familieHendelse.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null),
            dto.getTermindato());
    }
}
