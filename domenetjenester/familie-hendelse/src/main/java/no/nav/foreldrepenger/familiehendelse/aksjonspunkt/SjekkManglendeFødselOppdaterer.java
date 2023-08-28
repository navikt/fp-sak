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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFodselDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkManglendeFodselDto.class, adapter = AksjonspunktOppdaterer.class)
public class SjekkManglendeFødselOppdaterer implements AksjonspunktOppdaterer<SjekkManglendeFodselDto> {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    SjekkManglendeFødselOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public SjekkManglendeFødselOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                          SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.historikkAdapter = historikkAdapter;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId)
            .equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(SjekkManglendeFodselDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var grunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);
        var totrinn = håndterEndringHistorikk(dto, param.getRef(), param, grunnlag);
        var utledetResultat = utledFødselsdata(dto, grunnlag);

        var forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingId);
        oppdatertOverstyrtHendelse.tilbakestillBarn()
            .medAntallBarn(utledetResultat.size())
            .erFødsel() // Settes til fødsel for å sikre at typen blir fødsel selv om det ikke er født barn.
            .medErMorForSykVedFødsel(null);
        utledetResultat.forEach(
            it -> oppdatertOverstyrtHendelse.leggTilBarn(it.getFødselsdato(), it.getDødsdato().orElse(null)));

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);

        var skalReinnhenteRegisteropplysninger = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        if (skalReinnhenteRegisteropplysninger) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        }
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean håndterEndringHistorikk(SjekkManglendeFodselDto dto,
                                            BehandlingReferanse behandlingReferanse,
                                            AksjonspunktOppdaterParameter param,
                                            FamilieHendelseGrunnlagEntitet grunnlag) {
        var utledetResultat = utledFødselsdata(dto, grunnlag);
        var originalDokumentasjonForeligger = hentOrginalDokumentasjonForeligger(grunnlag);
        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.DOKUMENTASJON_FORELIGGER,
            originalDokumentasjonForeligger.orElse(null), dto.getDokumentasjonForeligger());

        erEndret = sjekkFødselsDatoOgAntallBarnEndret(grunnlag, utledetResultat, erEndret);
        var gjeldendeAntallBarn = dto.getDokumentasjonForeligger() ? dto.getUidentifiserteBarn().size() : grunnlag.getBekreftetVersjon()
            .map(FamilieHendelseEntitet::getAntallBarn)
            .orElse(0);

        opprettetInnslagforFeltBrukAntallBarnITps(dto, behandlingReferanse);

        historikkAdapter.tekstBuilder()
            .medOpplysning(HistorikkOpplysningType.ANTALL_BARN, gjeldendeAntallBarn)
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_FOEDSEL);

        return erEndret || grunnlag.getOverstyrtVersjon().isPresent();
    }

    private List<UidentifisertBarn> utledFødselsdata(SjekkManglendeFodselDto dto,
                                                     FamilieHendelseGrunnlagEntitet grunnlag) {
        var bekreftetVersjon = grunnlag.getBekreftetVersjon();

        var brukAntallBarnISøknad = dto.getDokumentasjonForeligger() && !dto.isBrukAntallBarnITps();
        var barn = brukAntallBarnISøknad ? konverterBarn(dto.getUidentifiserteBarn()) : bekreftetVersjon.map(FamilieHendelseEntitet::getBarna).orElse(List.of());
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
        return barn.stream()
            .map(b -> new AntallBarnOgFødselsdato(b.getFodselsdato(), b.getDodsdato(), 0))
            .collect(Collectors.toList()); //NOSONAR
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

    private Integer getAntallBarnVedSøknadFødsel(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAntallBarn();
    }

    private boolean sjekkFødselsDatoOgAntallBarnEndret(FamilieHendelseGrunnlagEntitet behandlingsgrunnlag,
                                                       List<UidentifisertBarn> dto,
                                                       boolean erEndret) {
        var erEndretTemp = dto.isEmpty() || erEndret;
        var orginalFødselsdato = behandlingsgrunnlag.getGjeldendeBarna().stream().map(UidentifisertBarn::getFødselsdato).min(LocalDate::compareTo).orElse(null);
        var dtoFødselsdato = dto.stream().map(UidentifisertBarn::getFødselsdato).min(LocalDate::compareTo).orElse(null);
        var originalDødsdatoer = behandlingsgrunnlag.getGjeldendeBarna().stream().map(UidentifisertBarn::getDødsdato).flatMap(Optional::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        var dtoDødsdatoer = dto.stream().map(UidentifisertBarn::getDødsdato).flatMap(Optional::stream).collect(Collectors.toCollection(LinkedHashSet::new));
        erEndretTemp = oppdaterVedEndretVerdi(HistorikkEndretFeltType.FODSELSDATO, orginalFødselsdato, dtoFødselsdato)
            || oppdaterVedEndretVerdi(HistorikkEndretFeltType.DODSDATO, originalDødsdatoer, dtoDødsdatoer)
            || erEndretTemp;
        var opprinneligAntallBarn = getAntallBarnVedSøknadFødsel(behandlingsgrunnlag);
        erEndretTemp = oppdaterVedEndretVerdi(HistorikkEndretFeltType.ANTALL_BARN, opprinneligAntallBarn, dto.size())
            || erEndretTemp;
        return erEndretTemp;
    }

    private void opprettetInnslagforFeltBrukAntallBarnITps(SjekkManglendeFodselDto dto, BehandlingReferanse behandlingReferanse) {
        if (dto.getDokumentasjonForeligger()) {
            var feltNavn = utledFeltNavn(dto, behandlingReferanse);
            historikkAdapter.tekstBuilder().medEndretFelt(feltNavn, null, true);
        }
    }

    private static HistorikkEndretFeltType utledFeltNavn(SjekkManglendeFodselDto dto, BehandlingReferanse behandlingReferanse) {
        if (dto.isBrukAntallBarnITps()) {
            return HistorikkEndretFeltType.BRUK_ANTALL_I_TPS;
        }
        return BehandlingType.REVURDERING.equals(behandlingReferanse.behandlingType()) ? HistorikkEndretFeltType.BRUK_ANTALL_I_VEDTAKET : HistorikkEndretFeltType.BRUK_ANTALL_I_SOKNAD;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                           LocalDate original,
                                           LocalDate bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                           Number original,
                                           Number bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                           Boolean original,
                                           Boolean bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType,
                                           Set<LocalDate> original,
                                           Set<LocalDate> bekreftet) {
        var originalEndretMin = original.stream().filter(d -> !bekreftet.contains(d)).min(LocalDate::compareTo).orElse(null);
        var dtoDødEndretMin = bekreftet.stream().filter(d -> !original.contains(d)).min(LocalDate::compareTo).orElse(null);

        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, originalEndretMin, dtoDødEndretMin);
            return true;
        }
        return false;
    }

    private static TekniskException kanIkkeUtledeGjeldendeFødselsdato() {
        return new KanIkkeUtledeGjeldendeFødselsdatoException("FP-475767", "Kan ikke utlede gjeldende "
            + "fødselsdato ved bekreftelse av fødsel");
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
