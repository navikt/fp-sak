package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.fp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.BekreftStartdatoForPerioden;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarStartdatoForPeriodenDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarStartdatoForPeriodenOppdaterer implements AksjonspunktOppdaterer<AvklarStartdatoForPeriodenDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    AvklarStartdatoForPeriodenOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarStartdatoForPeriodenOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                                YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarStartdatoForPeriodenDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        avbrytOverflødigOverstyrAksjonpunkt(behandling)
            .ifPresent(a -> resultatBuilder.medEkstraAksjonspunktResultat(a.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        LocalDate skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var førsteUttakDato = param.getSkjæringstidspunkt().getFørsteUttaksdato();
        LocalDate startdatoFraSoknad = dto.getStartdatoFraSoknad();
        if (!startdatoFraSoknad.equals(førsteUttakDato) || harValgtStartdatoSomErSenereEnnDatoFraInntektsmelding(param.getRef(), startdatoFraSoknad, skjæringstidspunkt)) {

            if (harValgtStartdatoSomErSenereEnnDatoFraInntektsmelding(param.getRef(), startdatoFraSoknad, skjæringstidspunkt)) {
                return OppdateringResultat.utenTransisjon().medBeholdAksjonspunktÅpent().build();
            } else {
                HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
                tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.STARTDATO_FRA_SOKNAD, førsteUttakDato, startdatoFraSoknad);
            }
        }
        ytelseFordelingTjeneste.aksjonspunktAvklarStartdatoForPerioden(behandling.getId(), new BekreftStartdatoForPerioden(startdatoFraSoknad));

        return resultatBuilder.build();
    }

    private boolean harValgtStartdatoSomErSenereEnnDatoFraInntektsmelding(BehandlingReferanse ref, LocalDate startdatoFraSoknad, LocalDate original) {
        return inntektsmeldingTjeneste.hentInntektsmeldinger(ref, original).stream()
            .anyMatch(im -> startdatoFraSoknad.isAfter(im.getStartDatoPermisjon().orElseThrow()));
    }

    private Optional<Aksjonspunkt> avbrytOverflødigOverstyrAksjonpunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO);
    }
}
