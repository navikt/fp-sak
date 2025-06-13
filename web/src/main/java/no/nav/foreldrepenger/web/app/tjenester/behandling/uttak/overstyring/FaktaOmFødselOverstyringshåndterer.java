package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.FaktaFødselTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaOmFødselDto.class, adapter = Overstyringshåndterer.class)
public class FaktaOmFødselOverstyringshåndterer implements Overstyringshåndterer<OverstyringFaktaOmFødselDto> {

    private static final Logger LOG = LoggerFactory.getLogger(FaktaOmFødselOverstyringshåndterer.class);

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

        if ((dto.getBarn() != null && !dto.getBarn().isEmpty()) || dto.getTermindato() != null) {
            LOG.info("Overstyrer fakta rundt fødsel for behandlingId {} til {}", behandlingId, dto);

            faktaFødselTjeneste.overstyrFaktaOmFødsel(behandlingId, dto);
            opprettHistorikkinnslag(ref, dto, familieHendelse);
        }

        return OppdateringResultat.utenOverhopp();
    }

    private void opprettHistorikkinnslag(BehandlingReferanse ref, OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medFagsakId(ref.fagsakId())
                .medBehandlingId(ref.behandlingId())
                .medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
                .addLinje(new HistorikkinnslagLinjeBuilder().tekst("Overstyrt fakta om fødsel"))
                .addLinje(dto.getBegrunnelse());

        if (dto.getBarn() != null && !dto.getBarn().isEmpty()) {
            historikkinnslag.addLinje(fraTilEquals("Antall barn", familieHendelse.getGjeldendeAntallBarn(), dto.getAntallBarn()));

            var dtoBarn = dto.getBarn();
            var gjeldendeBarn = familieHendelse.getGjeldendeVersjon().getBarna();

            if (dtoBarn != null && !gjeldendeBarn.isEmpty()) {
                if (dtoBarn.size() > gjeldendeBarn.size()) {
                    leggTilBarnHistorikk(historikkinnslag, dtoBarn, gjeldendeBarn);
                } else if (dtoBarn.size() < gjeldendeBarn.size()) {
                    fjernBarnHistorikk(historikkinnslag, dtoBarn, gjeldendeBarn);
                }
            }
        }

        var gjeldendeTerminDato = familieHendelse.getGjeldendeVersjon().getTermindato().orElse(null);
        if (dto.getTermindato() != null && !dto.getTermindato().equals(gjeldendeTerminDato)) {
            historikkinnslag.addLinje(lagTermindatoLinje(dto, familieHendelse));
        }

        historikkRepository.lagre(historikkinnslag.build());
    }

    private void leggTilBarnHistorikk(Historikkinnslag.Builder historikkinnslag, List<?> dtoBarn, List<?> overstyrtBarn) {
        var nyeBarn = new HashSet<>(dtoBarn);
        overstyrtBarn.forEach(nyeBarn::remove);
        nyeBarn.stream()
                .map(barn -> ((UidentifisertBarnDto) barn).getFodselsdato())
                .distinct()
                .forEach(fødselsdato -> historikkinnslag.addLinje(
                        new HistorikkinnslagLinjeBuilder().tekst("Barn lagt til med fødselsdato: " + fødselsdato)
                ));
    }

    private void fjernBarnHistorikk(Historikkinnslag.Builder historikkinnslag, List<?> dtoBarn, List<?> gjeldendeBarn) {
        var fjernedeBarn = new HashSet<>(gjeldendeBarn);
        dtoBarn.forEach(fjernedeBarn::remove);
        fjernedeBarn.stream()
                .map(barn -> ((UidentifisertBarnEntitet) barn).getFødselsdato())
                .distinct()
                .forEach(fødselsdato -> historikkinnslag.addLinje(
                        new HistorikkinnslagLinjeBuilder().tekst("Barn fjernet med fødselsdato: " + fødselsdato)
                ));
    }

    private static HistorikkinnslagLinjeBuilder lagTermindatoLinje(OverstyringFaktaOmFødselDto dto,
                                                                   FamilieHendelseGrunnlagEntitet familieHendelse) {
        return fraTilEquals("Termindato", familieHendelse.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null),
                dto.getTermindato());
    }
}
