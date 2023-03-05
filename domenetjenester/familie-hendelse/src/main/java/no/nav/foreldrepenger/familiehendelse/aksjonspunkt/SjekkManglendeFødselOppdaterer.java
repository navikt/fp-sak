package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
        var behandling = param.getBehandling();
        var grunnlag = familieHendelseTjeneste.hentAggregat(behandling.getId());
        var totrinn = håndterEndringHistorikk(dto, behandling, param, grunnlag);
        var utledetResultat = utledFødselsdata(dto, grunnlag);

        var forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(
            behandling.getId());

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse.tilbakestillBarn()
            .medAntallBarn(utledetResultat.size())
            .erFødsel() // Settes til fødsel for å sikre at typen blir fødsel selv om det ikke er født barn.
            .medErMorForSykVedFødsel(null);
        utledetResultat.forEach(
            it -> oppdatertOverstyrtHendelse.leggTilBarn(it.getFødselsdato(), it.getDødsdato().orElse(null)));

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);

        var skalReinnhenteRegisteropplysninger = skalReinnhenteRegisteropplysninger(param.getBehandlingId(),
            forrigeSkjæringstidspunkt);

        if (skalReinnhenteRegisteropplysninger) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).medOppdaterGrunnlag().build();
        }
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean håndterEndringHistorikk(SjekkManglendeFodselDto dto,
                                            Behandling behandling,
                                            AksjonspunktOppdaterParameter param,
                                            FamilieHendelseGrunnlagEntitet grunnlag) {
        var utledetResultat = utledFødselsdata(dto, grunnlag);
        var originalDokumentasjonForeligger = hentOrginalDokumentasjonForeligger(grunnlag);
        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.DOKUMENTASJON_FORELIGGER,
            originalDokumentasjonForeligger, dto.getDokumentasjonForeligger());

        int gjeldendeAntallBarn;

        erEndret = sjekkFødselsDatoOgAntallBarnEndret(grunnlag, utledetResultat, erEndret);
        gjeldendeAntallBarn = dto.getDokumentasjonForeligger() ? dto.getUidentifiserteBarn()
            .size() : grunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0);

        opprettetInnslagforFeltBrukAntallBarnITps(dto, behandling);

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
        var barn = brukAntallBarnISøknad ? konverterBarn(dto.getUidentifiserteBarn()) : bekreftetVersjon.map(
            FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        if (barn.stream().anyMatch(b -> null == b.getFødselsdato())) {
            throw kanIkkeUtledeGjeldendeFødselsdato();
        }
        return barn;
    }

    private List<UidentifisertBarn> konverterBarn(List<UidentifisertBarnDto> barn) {
        return barn.stream()
            .map(b -> new AntallBarnOgFødselsdato(b.getFodselsdato(), b.getDodsdato(), 0))
            .collect(Collectors.toList());
    }

    private Boolean hentOrginalDokumentasjonForeligger(FamilieHendelseGrunnlagEntitet grunnlag) {
        var overstyrtVersjon = grunnlag.getOverstyrtVersjon();
        var overstyrt = overstyrtVersjon.orElse(null);

        if (overstyrt != null && overstyrt.getType().equals(FamilieHendelseType.FØDSEL)) {
            var bekreftet = grunnlag.getBekreftetVersjon();
            if (bekreftet.isPresent()) {
                var familieHendelse = bekreftet.get();
                var antallBarnLike = Objects.equals(familieHendelse.getAntallBarn(), overstyrt.getAntallBarn());
                var fødselsdatoLike = Objects.equals(familieHendelse.getFødselsdato(), overstyrt.getFødselsdato());
                return (antallBarnLike && fødselsdatoLike);
            }
            return !overstyrt.getBarna().isEmpty();
        }
        return null;
    }

    private Integer getAntallBarnVedSøknadFødsel(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAntallBarn();
    }

    private boolean sjekkFødselsDatoOgAntallBarnEndret(FamilieHendelseGrunnlagEntitet behandlingsgrunnlag,
                                                       List<UidentifisertBarn> dto,
                                                       boolean erEndret) {
        var erEndretTemp = erEndret;
        var orginalFødselsdato = getFødselsdato(behandlingsgrunnlag);
        erEndretTemp =
            (0 == dto.size() || oppdaterVedEndretVerdi(HistorikkEndretFeltType.FODSELSDATO, orginalFødselsdato,
                dto.get(0).getFødselsdato())) || erEndretTemp;
        var opprinneligAntallBarn = getAntallBarnVedSøknadFødsel(behandlingsgrunnlag);
        erEndretTemp = oppdaterVedEndretVerdi(HistorikkEndretFeltType.ANTALL_BARN, opprinneligAntallBarn, dto.size())
            || erEndretTemp;
        return erEndretTemp;
    }

    private void opprettetInnslagforFeltBrukAntallBarnITps(SjekkManglendeFodselDto dto, Behandling behandling) {
        var feltNavn = dto.isBrukAntallBarnITps() ? HistorikkEndretFeltType.BRUK_ANTALL_I_TPS : (BehandlingType.REVURDERING
            .equals(
                behandling.getType()) ? HistorikkEndretFeltType.BRUK_ANTALL_I_VEDTAKET : HistorikkEndretFeltType.BRUK_ANTALL_I_SOKNAD);

        if (dto.getDokumentasjonForeligger()) {
            historikkAdapter.tekstBuilder().medEndretFelt(feltNavn, null, true);
        }
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

    private LocalDate getFødselsdato(FamilieHendelseGrunnlagEntitet grunnlag) {
        var fødselsdato = grunnlag.getGjeldendeBarna()
            .stream()
            .map(UidentifisertBarn::getFødselsdato)
            .min(LocalDate::compareTo);

        return fødselsdato.orElse(null);
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
