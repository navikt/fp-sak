package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.time.LocalDate;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftTerminbekreftelseAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftTerminbekreftelseOppdaterer implements AksjonspunktOppdaterer<BekreftTerminbekreftelseAksjonspunktDto> {

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;
    private BekreftTerminbekreftelseValidator bekreftTerminbekreftelseValidator;
    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseRepository familieHendelseRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    BekreftTerminbekreftelseOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftTerminbekreftelseOppdaterer(BehandlingRepositoryProvider repositoryProvider, HistorikkTjenesteAdapter historikkAdapter,
                                              SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                              FamilieHendelseTjeneste familieHendelseTjeneste,
                                              BekreftTerminbekreftelseValidator bekreftTerminbekreftelseValidator) {
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.historikkAdapter = historikkAdapter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.bekreftTerminbekreftelseValidator = bekreftTerminbekreftelseValidator;
        this.familieHendelseTjeneste = familieHendelseTjeneste;

    }

    @Override
    public boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return !skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId).equals(forrigeSkjæringstidspunkt);
    }

    @Override
    public OppdateringResultat oppdater(BekreftTerminbekreftelseAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        var behandlingId = param.getBehandlingId();
        final var grunnlag = familieHendelseRepository.hentAggregat(behandlingId);

        var orginalTermindato = getTermindato(grunnlag);
        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.TERMINDATO, orginalTermindato, dto.getTermindato());

        var orginalUtstedtDato = getUtstedtdato(grunnlag);
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.UTSTEDTDATO, orginalUtstedtDato, dto.getUtstedtdato()) || erEndret;

        var opprinneligAntallBarn = getAntallBarnVedSøknadTerminbekreftelse(grunnlag);
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.ANTALL_BARN, opprinneligAntallBarn, dto.getAntallBarn()) || erEndret;

        var kreverTotrinn = false;
        if (erEndret || grunnlag.getOverstyrtVersjon().isPresent()) {
            kreverTotrinn = true;
            if (erEndret) {
                opprettHistorikkinnslag(param, dto);
            }
        }

        final var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        if (FamilieHendelseType.TERMIN.equals(grunnlag.getGjeldendeVersjon().getType())) {
            oppdatertOverstyrtHendelse
                .tilbakestillBarn()
                .medTerminbekreftelse(oppdatertOverstyrtHendelse.getTerminbekreftelseBuilder()
                    .medNavnPå(grunnlag.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getNavnPå).orElse("Ukjent opphav"))
                    .medTermindato(dto.getTermindato())
                    .medUtstedtDato(dto.getUtstedtdato()))
                .medAntallBarn(dto.getAntallBarn());
            familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
        } else {
            // Hvis man nå av en eller annen grunn har aksjonspunkt avklar termin når typen er fødsel.
            oppdatertOverstyrtHendelse
                .medTerminbekreftelse(oppdatertOverstyrtHendelse.getTerminbekreftelseBuilder()
                    .medNavnPå(grunnlag.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getNavnPå).orElse("Ukjent opphav"))
                    .medTermindato(dto.getTermindato())
                    .medUtstedtDato(dto.getUtstedtdato()))
                .medAntallBarn(dto.getAntallBarn());
            familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);
        }

        final var oppdatertGrunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);

        final var forrigeSkjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        var skalReinnhente = skalReinnhenteRegisteropplysninger(behandlingId, forrigeSkjæringstidspunkt);

        var builder = OppdateringResultat.utenTransisjon().medTotrinnHvis(kreverTotrinn);
        if (skalReinnhente) {
            builder.medOppdaterGrunnlag();
        }
        if (FamilieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(oppdatertGrunnlag)) {
            // Må kontrollere fakta på nytt for å sjekke om fødsel skulle ha inntruffet.
            // TODO: Vurder å fjerne denne (var tilbakehopp til kofak). APutleder/StpUtl tar hensyn til utløpt frist. Oppdateres når tas opp i gui.
            builder.medOppdaterGrunnlag();
        }

        return builder.build();
    }

    private void opprettHistorikkinnslag(AksjonspunktOppdaterParameter param,
                                         BekreftTerminbekreftelseAksjonspunktDto dto) {
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_FOEDSEL);
        historikkAdapter.opprettHistorikkInnslag(param.getBehandlingId(), finnHistorikkinnslagType(dto));
    }

    private HistorikkinnslagType finnHistorikkinnslagType(BekreftTerminbekreftelseAksjonspunktDto dto) {
        var funnetFeil = bekreftTerminbekreftelseValidator.validerOpplysninger(dto);
        return funnetFeil ? HistorikkinnslagType.TERMINBEKREFTELSE_UGYLDIG : HistorikkinnslagType.FAKTA_ENDRET;
    }

    private Integer getAntallBarnVedSøknadTerminbekreftelse(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeAntallBarn();
    }

    private LocalDate getTermindato(FamilieHendelseGrunnlagEntitet grunnlag) {
        return getGjeldendeTerminbekreftelse(grunnlag).getTermindato();
    }

    private TerminbekreftelseEntitet getGjeldendeTerminbekreftelse(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeTerminbekreftelse()
            .orElseThrow(() -> new IllegalStateException("Har ikke terminbekreftelse når forventet"));
    }

    private LocalDate getUtstedtdato(FamilieHendelseGrunnlagEntitet grunnlag) {
        return getGjeldendeTerminbekreftelse(grunnlag).getUtstedtdato();
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, Number original, Number bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, LocalDate original, LocalDate bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }
}
