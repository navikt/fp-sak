package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFodselDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkManglendeFodselDto.class, adapter = AksjonspunktOppdaterer.class)
public class SjekkManglendeFødselOppdaterer implements AksjonspunktOppdaterer<SjekkManglendeFodselDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    SjekkManglendeFødselOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public SjekkManglendeFødselOppdaterer(OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          Historikkinnslag2Repository historikkinnslag2Repository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat oppdater(SjekkManglendeFodselDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var grunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        var totrinn = håndterEndringHistorikk(dto, param.getRef(), grunnlag);
        var utledetResultat = utledFødselsdata(dto, grunnlag);

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingId);
        oppdatertOverstyrtHendelse.tilbakestillBarn()
            .medAntallBarn(utledetResultat.size())
            .erFødsel() // Settes til fødsel for å sikre at typen blir fødsel selv om det ikke er født barn.
            .medErMorForSykVedFødsel(null);
        utledetResultat.forEach(it -> oppdatertOverstyrtHendelse.leggTilBarn(it.getFødselsdato(), it.getDødsdato().orElse(null)));

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);

        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        }
    }

    private boolean håndterEndringHistorikk(SjekkManglendeFodselDto dto,
                                            BehandlingReferanse behandlingReferanse,
                                            FamilieHendelseGrunnlagEntitet grunnlag) {
        var utledetResultat = utledFødselsdata(dto, grunnlag);
        var originalDokumentasjonForeligger = hentOrginalDokumentasjonForeligger(grunnlag);

        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId());

        var erEndret = oppdaterVedEndretVerdi(historikkinnslag, originalDokumentasjonForeligger.orElse(null), dto.getDokumentasjonForeligger());

        if (dto.getDokumentasjonForeligger()) {
            var gjeldendeAntallBarn = dto.getDokumentasjonForeligger() ? dto.getUidentifiserteBarn().size() : grunnlag.getBekreftetVersjon()
                .map(FamilieHendelseEntitet::getAntallBarn)
                .orElse(0);
            erEndret = sjekkFødselsDatoOgAntallBarnEndret(historikkinnslag, grunnlag, utledetResultat, erEndret);
            opprettetInnslagforFeltBrukAntallBarnIPDL(dto, historikkinnslag, behandlingReferanse);
            historikkinnslag.addTekstlinje(
                new HistorikkinnslagTekstlinjeBuilder().bold("Antall barn").tekst("som brukes i behandlingen:").bold(gjeldendeAntallBarn));
        }

        historikkinnslag.addTekstlinje(dto.getBegrunnelse());
        historikkinnslag2Repository.lagre(historikkinnslag.build());

        return erEndret || grunnlag.getOverstyrtVersjon().isPresent();
    }

    private List<UidentifisertBarn> utledFødselsdata(SjekkManglendeFodselDto dto, FamilieHendelseGrunnlagEntitet grunnlag) {
        var bekreftetVersjon = grunnlag.getBekreftetVersjon();

        var brukAntallBarnISøknad = dto.getDokumentasjonForeligger() && !dto.isBrukAntallBarnITps();
        var barn = brukAntallBarnISøknad ? konverterBarn(dto.getUidentifiserteBarn()) : bekreftetVersjon.map(FamilieHendelseEntitet::getBarna)
            .orElse(List.of());
        if (barn.stream().anyMatch(b -> null == b.getFødselsdato())) {
            throw kanIkkeUtledeGjeldendeFødselsdato();
        }
        return barn;
    }

    private List<UidentifisertBarn> konverterBarn(List<UidentifisertBarnDto> barn) {
        if (barn.stream().anyMatch(b -> b.getDodsdato().isPresent() && b.getDodsdato().get().isBefore(b.getFodselsdato()))) {
            // Finnes noen tilfelle i prod. Kan påvirke ytelsen
            throw new FunksjonellException("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
        }
        return barn.stream().map(b -> new AntallBarnOgFødselsdato(b.getFodselsdato(), b.getDodsdato(), 0)).collect(Collectors.toList()); //NOSONAR
    }

    private Optional<Boolean> hentOrginalDokumentasjonForeligger(FamilieHendelseGrunnlagEntitet grunnlag) {
        var overstyrtVersjon = grunnlag.getOverstyrtVersjon();
        var overstyrt = overstyrtVersjon.orElse(null);

        if (overstyrt != null && overstyrt.getType().equals(FamilieHendelseType.FØDSEL)) {
            var bekreftet = grunnlag.getBekreftetVersjon();
            if (bekreftet.isPresent()) {
                var familieHendelse = bekreftet.get();
                var antallBarnLike = Objects.equals(familieHendelse.getAntallBarn(), overstyrt.getAntallBarn());
                var fødselsdatoLike = Objects.equals(familieHendelse.getFødselsdato(), overstyrt.getFødselsdato());
                return Optional.of(antallBarnLike && fødselsdatoLike);
            }
            return Optional.of(!overstyrt.getBarna().isEmpty());
        }
        return Optional.empty();
    }

    private boolean sjekkFødselsDatoOgAntallBarnEndret(Historikkinnslag2.Builder historikkinnslag,
                                                       FamilieHendelseGrunnlagEntitet fhGrunnlag,
                                                       List<UidentifisertBarn> dto,
                                                       boolean erEndret) {
        var erEndretTemp = dto.isEmpty() || erEndret;
        var orginalFødselsdato = fhGrunnlag.getGjeldendeBarna()
            .stream()
            .map(UidentifisertBarn::getFødselsdato)
            .min(LocalDate::compareTo)
            .orElse(null);
        var dtoFødselsdato = dto.stream().map(UidentifisertBarn::getFødselsdato).min(LocalDate::compareTo).orElse(null);
        var originalDødsdatoer = fhGrunnlag.getGjeldendeBarna()
            .stream()
            .map(UidentifisertBarn::getDødsdato)
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        var dtoDødsdatoer = dto.stream()
            .map(UidentifisertBarn::getDødsdato)
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        erEndretTemp = oppdaterVedEndretVerdi(historikkinnslag, orginalFødselsdato, dtoFødselsdato) || erEndretTemp;
        erEndretTemp = oppdaterVedEndretVerdi(historikkinnslag, originalDødsdatoer, dtoDødsdatoer) || erEndretTemp;
        var opprinneligAntallBarn = fhGrunnlag.getGjeldendeAntallBarn();
        erEndretTemp = oppdaterVedEndretVerdi(historikkinnslag, opprinneligAntallBarn, dto.size()) || erEndretTemp;
        return erEndretTemp;
    }

    private void opprettetInnslagforFeltBrukAntallBarnIPDL(SjekkManglendeFodselDto dto,
                                                           Historikkinnslag2.Builder historikkinnslag,
                                                           BehandlingReferanse behandlingReferanse) {
        if (dto.getDokumentasjonForeligger()) {
            var feltNavn = utledFeltNavn(dto, behandlingReferanse);
            historikkinnslag.addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().til(utledFeltNavn(dto, behandlingReferanse).getNavn(), true));
        }
    }

    private static HistorikkEndretFeltType utledFeltNavn(SjekkManglendeFodselDto dto, BehandlingReferanse behandlingReferanse) {
        if (dto.isBrukAntallBarnITps()) {
            return HistorikkEndretFeltType.BRUK_ANTALL_I_TPS;
        }
        return BehandlingType.REVURDERING.equals(
            behandlingReferanse.behandlingType()) ? HistorikkEndretFeltType.BRUK_ANTALL_I_VEDTAKET : HistorikkEndretFeltType.BRUK_ANTALL_I_SOKNAD;
    }

    private boolean oppdaterVedEndretVerdi(Historikkinnslag2.Builder historikkinnslag, LocalDate original, LocalDate bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkinnslag.addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().fraTil("Fødselsdato", original, bekreftet));
            return true;
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(Historikkinnslag2.Builder historikkinnslag, Integer original, Integer bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkinnslag.addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().fraTil("Antall barn", original, bekreftet));
            return true;
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(Historikkinnslag2.Builder historikkinnslag, Boolean original, Boolean bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkinnslag.addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().fraTil("Dokumentasjon foreligger", original, bekreftet));
            return true;
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(Historikkinnslag2.Builder historikkinnslag, Set<LocalDate> original, Set<LocalDate> bekreftet) {
        var originalEndretMin = original.stream().filter(d -> !bekreftet.contains(d)).min(LocalDate::compareTo).orElse(null);
        var dtoDødEndretMin = bekreftet.stream().filter(d -> !original.contains(d)).min(LocalDate::compareTo).orElse(null);

        if (!Objects.equals(bekreftet, original)) {
            historikkinnslag.addTekstlinje(new HistorikkinnslagTekstlinjeBuilder().fraTil("Dødsdato", originalEndretMin, dtoDødEndretMin));
            return true;
        }
        return false;
    }

    private static TekniskException kanIkkeUtledeGjeldendeFødselsdato() {
        return new KanIkkeUtledeGjeldendeFødselsdatoException("FP-475767", "Kan ikke utlede gjeldende " + "fødselsdato ved bekreftelse av fødsel");
    }

    private static class AntallBarnOgFødselsdato implements UidentifisertBarn {
        private final LocalDate fødselsdato;
        private final LocalDate dødsdato;
        private final Integer barnNummer;

        AntallBarnOgFødselsdato(LocalDate fødselsdato, Optional<LocalDate> dødsdato, Integer barnNummer) {
            this.fødselsdato = fødselsdato;
            this.dødsdato = dødsdato.orElse(null);
            this.barnNummer = barnNummer;
        }

        @Override
        public LocalDate getFødselsdato() {
            return fødselsdato;
        }

        @Override
        public Optional<LocalDate> getDødsdato() {
            return Optional.ofNullable(dødsdato);
        }

        @Override
        public Integer getBarnNummer() {
            return barnNummer;
        }
    }
}
