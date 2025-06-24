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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFodselDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.DATE_FORMATTER;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkManglendeFodselDto.class, adapter = AksjonspunktOppdaterer.class)
public class SjekkManglendeFødselOppdaterer implements AksjonspunktOppdaterer<SjekkManglendeFodselDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    SjekkManglendeFødselOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public SjekkManglendeFødselOppdaterer(OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          HistorikkinnslagRepository historikkinnslagRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(SjekkManglendeFodselDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var grunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);


        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId);
            oppdatertOverstyrtHendelse.erFødsel();

        if (Boolean.TRUE.equals(dto.getDokumentasjonForeligger())) {
            var utledetResultat = utledFødselsdata(dto, grunnlag);
            oppdatertOverstyrtHendelse.tilbakestillBarn()
                .medAntallBarn(utledetResultat.size())
                .erFødsel() // Settes til fødsel for å sikre at typen blir fødsel selv om det ikke er født barn.
                .medErMorForSykVedFødsel(null);
            utledetResultat.forEach(it -> oppdatertOverstyrtHendelse.leggTilBarn(it.getFødselsdato(), it.getDødsdato().orElse(null)));

            familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
        }

        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());

        var totrinn = erEndring(dto, grunnlag);
        opprettHistorikkinnslag(dto, param.getRef(), grunnlag);

        if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        }
    }


    private boolean erEndring(SjekkManglendeFodselDto dto, FamilieHendelseGrunnlagEntitet grunnlag) {
        var originalDokumentasjonForeligger = hentOriginalDokumentasjonForeligger(grunnlag);

        var originalFødselStatus = grunnlag.getGjeldendeBarna().stream().map(FødselStatus::new).sorted().toList();

        var bekreftetFødselStatus = dto.getUidentifiserteBarn().stream().map(FødselStatus::new).sorted().toList();

        return !Objects.equals(originalDokumentasjonForeligger.orElse(null), dto.getDokumentasjonForeligger()) || !Objects.equals(
            originalFødselStatus, bekreftetFødselStatus) || grunnlag.getOverstyrtVersjon().isPresent();
    }

    public boolean erEndret(List<FødselStatus> original, List<FødselStatus> bekreftet) {
        if (original.size() != bekreftet.size()) {
            return true;
        }
        var originalSortert = original.stream().sorted().toList();
        var bekreftetSortert = bekreftet.stream().sorted().toList();

        return !Objects.equals(originalSortert, bekreftetSortert);
    }

    private List<? extends UidentifisertBarn> utledFødselsdata(SjekkManglendeFodselDto dto, FamilieHendelseGrunnlagEntitet grunnlag) {
        var termindato = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        var bekreftetVersjon = grunnlag.getBekreftetVersjon();

        var brukAntallBarnISøknad = dto.getDokumentasjonForeligger();
        var barn = brukAntallBarnISøknad ? konverterBarn(dto.getUidentifiserteBarn()) : bekreftetVersjon.map(FamilieHendelseEntitet::getBarna)
            .orElse(List.of());

        if (barn.stream().anyMatch(b -> null == b.getFødselsdato())) {
            throw kanIkkeUtledeGjeldendeFødselsdato();
        }
        var fødselsdato = barn.stream().map(UidentifisertBarn::getFødselsdato).min(Comparator.naturalOrder());
        if (termindato.isPresent() && fødselsdato.isPresent()) {
            var fødselsintervall = FamilieHendelseTjeneste.intervallForTermindato(termindato.get());
            if (!fødselsintervall.encloses(fødselsdato.get())) {
                throw new FunksjonellException("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
            }
        }
        return barn;
    }

    private List<FødselStatus> konverterBarn(List<UidentifisertBarnDto> barn) {
        if (barn.stream().anyMatch(b -> b.getDødsdato().isPresent() && b.getDødsdato().get().isBefore(b.getFødselsdato()))) {
            // Finnes noen tilfelle i prod. Kan påvirke ytelsen
            throw new FunksjonellException("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
        }
        return barn.stream().map(FødselStatus::new).sorted().toList();
    }

    private Optional<Boolean> hentOriginalDokumentasjonForeligger(FamilieHendelseGrunnlagEntitet grunnlag) {
        if (grunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getGjelderFødsel).orElse(false)) {
            var overstyrt = grunnlag.getOverstyrtVersjon().get();

            if (grunnlag.getBekreftetVersjon().isPresent()) {
                var folkeregister = grunnlag.getBekreftetVersjon().get();
                return Optional.of(
                    Objects.equals(folkeregister.getAntallBarn(), overstyrt.getAntallBarn()) && Objects.equals(folkeregister.getFødselsdato(),
                        overstyrt.getFødselsdato()));
            } else {
                return Optional.of(!overstyrt.getBarna().isEmpty());
            }
        }
        return Optional.empty();
    }

    private void opprettHistorikkinnslag(SjekkManglendeFodselDto dto,
                                         BehandlingReferanse behandlingReferanse,
                                         FamilieHendelseGrunnlagEntitet grunnlag) {
        var originalDokumentasjonForeligger = hentOriginalDokumentasjonForeligger(grunnlag);

        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId());

        if (!Objects.equals(dto.getDokumentasjonForeligger(), originalDokumentasjonForeligger.orElse(null))) {
            historikkinnslag.addLinje(new HistorikkinnslagLinjeBuilder().fraTil("Finnes det dokumentasjon på at barnet er født?",
                originalDokumentasjonForeligger.orElse(null), dto.getDokumentasjonForeligger()));
        }

        if (dto.getDokumentasjonForeligger()) {
            lagHistorikkForBarn(historikkinnslag, grunnlag, dto);
        }

        historikkinnslag.addLinje(dto.getBegrunnelse());
        historikkinnslagRepository.lagre(historikkinnslag.build());
    }

    private void lagHistorikkForBarn(Historikkinnslag.Builder historikkinnslag,
                                     FamilieHendelseGrunnlagEntitet grunnlag,
                                     SjekkManglendeFodselDto dto) {
        var oppdatertFødselStatus = dto.getUidentifiserteBarn().stream().map(FødselStatus::new).sorted().toList();
        var gjeldendeFødselStatus = grunnlag.getGjeldendeBarna().stream().map(FødselStatus::new).sorted().toList();

        if (!Objects.equals(oppdatertFødselStatus.size(), grunnlag.getGjeldendeAntallBarn())) {
            historikkinnslag.addLinje(
                new HistorikkinnslagLinjeBuilder().fraTil("Antall barn", grunnlag.getGjeldendeAntallBarn(), oppdatertFødselStatus.size()));
        } else {
            historikkinnslag.addLinje(
                new HistorikkinnslagLinjeBuilder().bold("Antall barn").tekst("som brukes i behandlingen:").bold(oppdatertFødselStatus.size()));
        }

        if (!Objects.equals(gjeldendeFødselStatus, oppdatertFødselStatus)) {
            for (int i = 0; i < oppdatertFødselStatus.size(); i++) {
                var til = oppdatertFødselStatus.get(i).formaterLevetid();
                var fra = safeGet(gjeldendeFødselStatus, i).map(FødselStatus::formaterLevetid).orElse(null);
                var barn = oppdatertFødselStatus.size() > 1 ? "Barn " + (i + 1) : "Barn";
                historikkinnslag.addLinje(fraTilEquals(barn, fra, til));
            }
        }
        historikkinnslag.addLinje(utledKildeForBarn(oppdatertFødselStatus, grunnlag));
    }

    private String utledKildeForBarn(List<FødselStatus> oppdatertFødselStatus, FamilieHendelseGrunnlagEntitet grunnlag) {
        var søknadFødselStatus = grunnlag.getSøknadVersjon().getBarna().stream().map(FødselStatus::new).sorted().toList();

        var fregFødselStatus = grunnlag.getBekreftetVersjon()
            .map(FamilieHendelseEntitet::getBarna)
            .orElse(List.of())
            .stream()
            .map(FødselStatus::new)
            .sorted()
            .toList();

        if (Objects.equals(søknadFødselStatus, oppdatertFødselStatus)) {
            return "Barn er hentet fra søknad";
        } else if (Objects.equals(fregFødselStatus, oppdatertFødselStatus)) {
            return "Barn er hentet fra Folkergisteret";
        } else {
            return "Barn er endret manuelt";
        }
    }

    public static Optional<FødselStatus> safeGet(List<FødselStatus> list, int index) {
        return (index < list.size()) ? Optional.ofNullable(list.get(index)) : Optional.empty();
    }

    private static TekniskException kanIkkeUtledeGjeldendeFødselsdato() {
        return new KanIkkeUtledeGjeldendeFødselsdatoException("FP-475767", "Kan ikke utlede gjeldende " + "fødselsdato ved bekreftelse av fødsel");
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

        FødselStatus(UidentifisertBarnDto barn) {
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
            return getDødsdato().map(dødsdato -> String.format("f. %s - d. %s", fødselsdato.format(DATE_FORMATTER), dødsdato.format(DATE_FORMATTER)))
                .orElseGet(() -> String.format("f. %s", fødselsdato.format(DATE_FORMATTER)));
        }

        @Override
        public Integer getBarnNummer() {
            return barnNummer;
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
