package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFodselDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftetBarnDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.DATE_FORMATTER;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkManglendeFodselDto.class, adapter = AksjonspunktOppdaterer.class)
public class SjekkManglendeFødselOppdaterer implements AksjonspunktOppdaterer<SjekkManglendeFodselDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository behandlingRepository;

    SjekkManglendeFødselOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public SjekkManglendeFødselOppdaterer(OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          HistorikkinnslagRepository historikkinnslagRepository,
                                          BehandlingRepository behandlingRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(SjekkManglendeFodselDto dto, AksjonspunktOppdaterParameter param) {
        valider(dto);

        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var grunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);


        if (Boolean.TRUE.equals(dto.getErBarnFødt())) {
            var utledetResultat = utledFødselsdata(dto, grunnlag);
            var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId)
                .tilbakestillBarn()
                .medAntallBarn(utledetResultat.size())
                .erFødsel() // Settes til fødsel for å sikre at typen blir fødsel selv om det ikke er født barn.
                .medErMorForSykVedFødsel(null);
            utledetResultat.forEach(it -> oppdatertOverstyrtHendelse.leggTilBarn(it.getFødselsdato(), it.getDødsdato().orElse(null)));

            familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
        }

        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());

        opprettHistorikkinnslag(dto, param.getRef(), grunnlag, behandling);

        if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            return OppdateringResultat.utenTransisjon().medTotrinn().build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinn().medOppdaterGrunnlag().build();
        }
    }

    private void valider(SjekkManglendeFodselDto dto) {
        if (Boolean.TRUE.equals(dto.getErBarnFødt())) {
            if (dto.getBarn() == null || dto.getBarn().isEmpty()) {
                throw new FunksjonellException("FP-076343", "Mangler barn", "Oppgi mellom 1 og 9 barn");
            }
            if (dto.getBarn().size() > 9) {
                throw new FunksjonellException("FP-076347", "For mange barn", "Oppgi mellom 1 og 9 barn");
            }
            if (dto.getBarn().stream().anyMatch(b -> b.getDødsdato().isPresent() && b.getDødsdato().get().isBefore(b.getFødselsdato()))) {
                throw new FunksjonellException("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
            }
        }
    }

    private List<? extends UidentifisertBarn> utledFødselsdata(SjekkManglendeFodselDto dto, FamilieHendelseGrunnlagEntitet grunnlag) {
        var termindato = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

        var barn = dto.getBarn().stream().map(FødselStatus::new).sorted().toList();

        var fødselsdato = barn.stream().map(UidentifisertBarn::getFødselsdato).min(Comparator.naturalOrder());
        if (termindato.isPresent() && fødselsdato.isPresent()) {
            var fødselsintervall = FamilieHendelseTjeneste.intervallForTermindato(termindato.get());
            if (!fødselsintervall.encloses(fødselsdato.get())) {
                throw new FunksjonellException("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
            }
        }
        return barn;
    }

    private Optional<Boolean> hentOriginalErBarnetFødt(FamilieHendelseGrunnlagEntitet grunnlag, Behandling behandling) {
        var harUtførtAP = behandling.harUtførtAksjonspunktMedType(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL);

        if (!harUtførtAP) {
            return Optional.empty();
        }

        return Optional.of(grunnlag.getOverstyrtVersjon().filter(o -> !o.getBarna().isEmpty()).isPresent());
    }

    private void opprettHistorikkinnslag(SjekkManglendeFodselDto dto,
                                         BehandlingReferanse behandlingReferanse,
                                         FamilieHendelseGrunnlagEntitet grunnlag,
                                         Behandling behandling) {
        var originalErBarnetFødt = hentOriginalErBarnetFødt(grunnlag, behandling).orElse(null);

        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId());

        if (!Objects.equals(dto.getErBarnFødt(), originalErBarnetFødt)) {
            historikkinnslag.addLinje(
                new HistorikkinnslagLinjeBuilder().fraTil("Er barnet født?", originalErBarnetFødt,
                    dto.getErBarnFødt()));
        }

        if (Boolean.TRUE.equals(dto.getErBarnFødt())) {
            lagHistorikkForBarn(historikkinnslag, grunnlag, dto);
        }

        historikkinnslag.addLinje(dto.getBegrunnelse());

        historikkinnslagRepository.lagre(historikkinnslag.build());
    }

    private void lagHistorikkForBarn(Historikkinnslag.Builder historikkinnslag,
                                     FamilieHendelseGrunnlagEntitet grunnlag,
                                     SjekkManglendeFodselDto dto) {
        var oppdatertFødselStatus = dto.getBarn().stream().map(FødselStatus::new).sorted().toList();
        var gjeldendeFødselStatus = grunnlag.getGjeldendeBarna().stream().map(FødselStatus::new).sorted().toList();

        if (!Objects.equals(oppdatertFødselStatus.size(), grunnlag.getGjeldendeAntallBarn())) {
            historikkinnslag.addLinje(
                new HistorikkinnslagLinjeBuilder().fraTil("Antall barn", grunnlag.getGjeldendeAntallBarn(), oppdatertFødselStatus.size()));
        } else {
            historikkinnslag.addLinje(
                new HistorikkinnslagLinjeBuilder().bold("Antall barn").tekst("som brukes i behandlingen:").bold(oppdatertFødselStatus.size()));
        }

        if (!oppdatertFødselStatus.equals(gjeldendeFødselStatus)) {
            for (int i = 0; i < oppdatertFødselStatus.size(); i++) {
                var til = oppdatertFødselStatus.get(i).formaterLevetid();
                var fra = safeGet(gjeldendeFødselStatus, i).map(FødselStatus::formaterLevetid).orElse(null);
                var barn = oppdatertFødselStatus.size() > 1 ? "Barn " + (i + 1) : "Barn";
                historikkinnslag.addLinje(fraTilEquals(barn, fra, til));
            }
        }
    }

    public static Optional<FødselStatus> safeGet(List<FødselStatus> list, int index) {
        return (index < list.size()) ? Optional.ofNullable(list.get(index)) : Optional.empty();
    }

    public static class FødselStatus implements UidentifisertBarn, Comparable<FødselStatus> {
        private final LocalDate fødselsdato;
        private final LocalDate dødsdato;
        private final Integer barnNummer;

        FødselStatus(UidentifisertBarn barn) {
            this.fødselsdato = barn.getFødselsdato();
            this.dødsdato = barn.getDødsdato().orElse(null);
            this.barnNummer = barn.getBarnNummer();
        }

        FødselStatus(BekreftetBarnDto barn) {
            this.fødselsdato = barn.getFødselsdato();
            this.dødsdato = barn.getDødsdato().orElse(null);
            this.barnNummer = 0;
        }

        public LocalDate getFødselsdato() {
            return fødselsdato;
        }

        public Optional<LocalDate> getDødsdato() {
            return Optional.ofNullable(dødsdato);
        }

        public String formaterLevetid() {
            return getDødsdato().map(d -> String.format("f. %s - d. %s", fødselsdato.format(DATE_FORMATTER), d.format(DATE_FORMATTER)))
                .orElseGet(() -> String.format("f. %s", fødselsdato.format(DATE_FORMATTER)));
        }

        @Override
        public Integer getBarnNummer() {
            return barnNummer;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fødselsdato, dødsdato, barnNummer);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FødselStatus that)) {
                return false;
            }
            return Objects.equals(fødselsdato, that.fødselsdato) && Objects.equals(dødsdato, that.dødsdato);
        }

        @Override
        public int compareTo(FødselStatus other) {
            return Comparator.comparing((FødselStatus p) -> p.fødselsdato)
                .thenComparing(p -> p.dødsdato, Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(this, other);
        }
    }
}
