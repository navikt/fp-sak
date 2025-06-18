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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    private void leggTilDødsdatoEndretHistorikk(Historikkinnslag.Builder historikkinnslag, List<?> dtoBarn, List<?> gjeldendeBarn) {
        if (dtoBarn.size() != gjeldendeBarn.size()) {
            return; // Ikke logg endringer i fødselsdato/dødsdato når antall barn endres
        }

        var originalDødsdato = gjeldendeBarn.stream()
            .map(b -> ((UidentifisertBarnEntitet) b).getDødsdato())
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        var dtoDødsdato = dtoBarn.stream()
            .map(b -> ((UidentifisertBarnDto) b).getDodsdato())
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        oppdaterVedEndretVerdi(historikkinnslag, originalDødsdato, dtoDødsdato);
    }

    private void opprettHistorikkinnslag(BehandlingReferanse ref, OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .addLinje(new HistorikkinnslagLinjeBuilder().bold("Overstyrt fakta om fødsel"));

        if (dto.getBarn() != null && !dto.getBarn().isEmpty()) {
            historikkinnslag.addLinje(fraTilEquals("Antall barn", familieHendelse.getGjeldendeAntallBarn(), dto.getAntallBarn()));

            var dtoBarn = dto.getBarn();
            var gjeldendeBarn = familieHendelse.getGjeldendeVersjon().getBarna();

            if (dtoBarn != null && !gjeldendeBarn.isEmpty()) {
                if (dtoBarn.size() > gjeldendeBarn.size()) {
                    leggTilBarnHistorikk(historikkinnslag, dtoBarn, gjeldendeBarn);
                } else if (dtoBarn.size() < gjeldendeBarn.size()) {
                    fjernBarnHistorikk(historikkinnslag, dtoBarn, gjeldendeBarn);
                } else if (erKunDødsdatoEndret(dtoBarn, gjeldendeBarn)) {
                    leggTilDødsdatoEndretHistorikk(historikkinnslag, dtoBarn, gjeldendeBarn);
                } else if (erBarnEndret(dtoBarn, gjeldendeBarn)) {
                    fjernBarnHistorikk(historikkinnslag, dtoBarn, gjeldendeBarn);
                    leggTilBarnHistorikk(historikkinnslag, dtoBarn, gjeldendeBarn);
                }
            }
        }

        var gjeldendeTerminDato = familieHendelse.getGjeldendeVersjon().getTermindato().orElse(null);
        if (dto.getTermindato() != null && !dto.getTermindato().equals(gjeldendeTerminDato)) {
            historikkinnslag.addLinje(lagTermindatoLinje(dto, familieHendelse));
        }

        if (dto.getBarn() != null && dto.getAntallBarn() > 0) {
            historikkinnslag.addLinje(
                    new HistorikkinnslagLinjeBuilder().bold("Antall barn").tekst("som brukes i behandlingen:").bold(dto.getAntallBarn()));
        }
        historikkinnslag.addLinje(dto.getBegrunnelse());
        historikkRepository.lagre(historikkinnslag.build());
    }

    private boolean erKunDødsdatoEndret(List<?> dtoBarn, List<?> gjeldendeBarn) {
        for (int i = 0; i < dtoBarn.size(); i++) {
            var dtoBarnDto = (UidentifisertBarnDto) dtoBarn.get(i);
            var gjeldendeBarnEntitet = (UidentifisertBarnEntitet) gjeldendeBarn.get(i);

            if (!dtoBarnDto.getFodselsdato().equals(gjeldendeBarnEntitet.getFødselsdato())) {
                return false;
            }
            if (!Objects.equals(dtoBarnDto.getDodsdato(), gjeldendeBarnEntitet.getDødsdato())) {
                return true;
            }
        }
        return false;
    }

    private boolean erBarnEndret(List<?> dtoBarn, List<?> gjeldendeBarn) {
        for (int i = 0; i < dtoBarn.size(); i++) {
            var dtoBarnDto = (UidentifisertBarnDto) dtoBarn.get(i);
            var gjeldendeBarnEntitet = (UidentifisertBarnEntitet) gjeldendeBarn.get(i);

            if (!dtoBarnDto.getFodselsdato().equals(gjeldendeBarnEntitet.getFødselsdato()) || !Objects.equals(dtoBarnDto.getDodsdato(),
                gjeldendeBarnEntitet.getDødsdato())) {
                return true;
            }
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(Historikkinnslag.Builder historikkinnslag, Set<LocalDate> original, Set<LocalDate> bekreftet) {
        var originalEndretMin = original.stream().filter(d -> !bekreftet.contains(d)).min(LocalDate::compareTo).orElse(null);
        var dtoDødEndretMin = bekreftet.stream().filter(d -> !original.contains(d)).min(LocalDate::compareTo).orElse(null);

        if (!Objects.equals(bekreftet, original)) {
            historikkinnslag.addLinje(new HistorikkinnslagLinjeBuilder().fraTil("Dødsdato", originalEndretMin, dtoDødEndretMin));
            return true;
        }
        return false;
    }

    private void leggTilBarnHistorikk(Historikkinnslag.Builder historikkinnslag, List<?> dtoBarn, List<?> gjeldendeBarn) {
        var nyeBarn = new HashSet<>(dtoBarn);
        for (Object gjeldendeB : gjeldendeBarn) {
            var gjeldendeBarnEntitet = (UidentifisertBarnEntitet) gjeldendeB;
            boolean funnetMatch = false;
            var iterator = nyeBarn.iterator();
            while (iterator.hasNext() && !funnetMatch) {
                var dtoB = (UidentifisertBarnDto) iterator.next();
                if (dtoB.getFodselsdato().equals(gjeldendeBarnEntitet.getFødselsdato()) && dtoB.getDodsdato()
                    .equals(gjeldendeBarnEntitet.getDødsdato())) {
                    iterator.remove();
                    funnetMatch = true;
                }
            }
        }

        nyeBarn.stream().map(barn -> (UidentifisertBarnDto) barn).forEach(barnDto -> {
            var builder = new HistorikkinnslagLinjeBuilder().bold("Barn lagt til").tekst("med fødselsdato:").bold(barnDto.getFodselsdato());
            if (barnDto.getDodsdato().isPresent()) {
                builder.tekst("og dødsdato:").bold(barnDto.getDodsdato().get());
            }
            historikkinnslag.addLinje(builder);
        });
    }

    private void fjernBarnHistorikk(Historikkinnslag.Builder historikkinnslag, List<?> dtoBarn, List<?> gjeldendeBarn) {
        var fjernedeBarn = new HashSet<>(gjeldendeBarn);
        for (Object dtoB : dtoBarn) {
            var dtoBarnDto = (UidentifisertBarnDto) dtoB;
            boolean funnetMatch = false;
            var iterator = fjernedeBarn.iterator();
            while (iterator.hasNext() && !funnetMatch) {
                var gjeldendeBarnEntitet = (UidentifisertBarnEntitet) iterator.next();
                if (dtoBarnDto.getFodselsdato().equals(gjeldendeBarnEntitet.getFødselsdato()) && dtoBarnDto.getDodsdato()
                    .equals(gjeldendeBarnEntitet.getDødsdato())) {
                    iterator.remove();
                    funnetMatch = true;
                }
            }
        }

        fjernedeBarn.stream().map(barn -> (UidentifisertBarnEntitet) barn).forEach(barnEntitet -> {
            var builder = new HistorikkinnslagLinjeBuilder().bold("Barn fjernet").tekst("med fødselsdato:").bold(barnEntitet.getFødselsdato());
            if (barnEntitet.getDødsdato().isPresent()) {
                builder.tekst("og dødsdato:").bold(barnEntitet.getDødsdato().get());
            }
            historikkinnslag.addLinje(builder);
        });
    }

    private static HistorikkinnslagLinjeBuilder lagTermindatoLinje(OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        return fraTilEquals("Termindato", familieHendelse.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null),
            dto.getTermindato());
    }
}
