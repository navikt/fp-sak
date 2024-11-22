package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftTerminbekreftelseAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftTerminbekreftelseOppdaterer implements AksjonspunktOppdaterer<BekreftTerminbekreftelseAksjonspunktDto> {


    private Historikkinnslag2Repository historikkinnslagRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    BekreftTerminbekreftelseOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftTerminbekreftelseOppdaterer(Historikkinnslag2Repository historikkinnslagRepository,
                                              OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                              FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftTerminbekreftelseAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingReferanse = param.getRef();
        var behandlingId = behandlingReferanse.behandlingId();
        var grunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, behandlingReferanse.fagsakYtelseType());

        var erEndret = !Objects.equals(getTermindato(grunnlag), dto.getTermindato()) ||
            !Objects.equals(getUtstedtdato(grunnlag), dto.getUtstedtdato()) ||
            !Objects.equals(getAntallBarnVedSøknadTerminbekreftelse(grunnlag), dto.getAntallBarn());

        if (erEndret || grunnlag.getOverstyrtVersjon().isEmpty()) {
            var historikkinnslag = lagHistorikkinnslag(dto, behandlingReferanse, grunnlag, erEndret);
            historikkinnslagRepository.lagre(historikkinnslag);
        }

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandlingId);
        if (FamilieHendelseType.TERMIN.equals(grunnlag.getGjeldendeVersjon().getType())) {
            oppdatertOverstyrtHendelse
                .tilbakestillBarn()
                .medTerminbekreftelse(oppdatertOverstyrtHendelse.getTerminbekreftelseBuilder()
                    .medNavnPå(grunnlag.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getNavnPå).orElse("Ukjent opphav"))
                    .medTermindato(dto.getTermindato())
                    .medUtstedtDato(dto.getUtstedtdato()))
                .medAntallBarn(dto.getAntallBarn());
            familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
        } else {
            // Hvis man nå av en eller annen grunn har aksjonspunkt avklar termin når typen er fødsel.
            oppdatertOverstyrtHendelse
                .medTerminbekreftelse(oppdatertOverstyrtHendelse.getTerminbekreftelseBuilder()
                    .medNavnPå(grunnlag.getSøknadVersjon().getTerminbekreftelse().map(TerminbekreftelseEntitet::getNavnPå).orElse("Ukjent opphav"))
                    .medTermindato(dto.getTermindato())
                    .medUtstedtDato(dto.getUtstedtdato()))
                .medAntallBarn(dto.getAntallBarn());
            familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
        }
        var kreverTotrinn = erEndret || grunnlag.getOverstyrtVersjon().isPresent();
        var builder = OppdateringResultat.utenTransisjon().medTotrinnHvis(kreverTotrinn);
        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, behandlingReferanse.fagsakYtelseType());
        if (!Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            builder.medOppdaterGrunnlag().build();
        }
        if (FamilieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(familieHendelseTjeneste.hentAggregat(behandlingId))) {
            // Må kontrollere fakta på nytt for å sjekke om fødsel skulle ha inntruffet.
            builder.medOppdaterGrunnlag();
        }

        return builder.build();
    }

    private Historikkinnslag2 lagHistorikkinnslag(BekreftTerminbekreftelseAksjonspunktDto dto,
                                                  BehandlingReferanse behandlingReferanse,
                                                  FamilieHendelseGrunnlagEntitet grunnlag,
                                                  boolean erEndret) {
        var historikkinnslagBuilder = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL);
        if (erEndret) {
            return historikkinnslagBuilder
                .addTekstlinje(fraTilEquals("Termindato", getTermindato(grunnlag), dto.getTermindato()))
                .addTekstlinje(fraTilEquals("Utstedtdato", getUtstedtdato(grunnlag), dto.getUtstedtdato()))
                .addTekstlinje(fraTilEquals("Antall barn", getAntallBarnVedSøknadTerminbekreftelse(grunnlag), dto.getAntallBarn()))
                .addTekstlinje(dto.getBegrunnelse())
                .build();
        } else {
            return historikkinnslagBuilder
                .addTekstlinje(fraTilEquals("Terminbekreftelse", null, "godkjent"))
                .addTekstlinje(dto.getBegrunnelse())
                .build();
        }
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
}
