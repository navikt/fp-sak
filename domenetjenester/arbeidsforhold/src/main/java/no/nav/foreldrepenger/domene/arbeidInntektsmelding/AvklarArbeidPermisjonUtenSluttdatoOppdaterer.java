package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.util.Comparator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk.ArbeidPermHistorikkInnslagTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftArbeidMedPermisjonUtenSluttdatoDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarArbeidPermisjonUtenSluttdatoOppdaterer implements AksjonspunktOppdaterer<BekreftArbeidMedPermisjonUtenSluttdatoDto> {
    @Valid
    @Size(max = 1000)
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private ArbeidPermHistorikkInnslagTjeneste arbeidPermHistorikkInnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    AvklarArbeidPermisjonUtenSluttdatoOppdaterer(){
        //CDI
    }

    @Inject
    public AvklarArbeidPermisjonUtenSluttdatoOppdaterer(ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                                        ArbeidPermHistorikkInnslagTjeneste arbeidPermHistorikkInnslagTjeneste,
                                                        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.arbeidPermHistorikkInnslagTjeneste = arbeidPermHistorikkInnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftArbeidMedPermisjonUtenSluttdatoDto bekreftetArbforholdDto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Finner ikke arbeidsinformasjon for behanldingId: "+ behandlingId));

        var arbeidsforholdInformasjonBuilder = arbeidsforholdAdministrasjonTjeneste.opprettBuilderFor(behandlingId).tilbakestillOverstyringer();

        for (var avklartArbeidsForhold : bekreftetArbforholdDto.getArbeidsforhold()) {
            if (erGyldigPermisjonStatus(avklartArbeidsForhold.permisjonStatus())) {
                var internArbeidsforholdId = avklartArbeidsForhold.internArbeidsforholdId();
                var arbeidsgiver = hentArbeidsgiver(avklartArbeidsForhold.arbeidsgiverIdent());

                var yrkesaktiviet = hentYrkesaktivietForArbeidsforholdet(param, iayGrunnlag, avklartArbeidsForhold);
                var permisjonsperiode = hentPermisjonsperiodeForArbeidsforholdet(avklartArbeidsForhold, yrkesaktiviet);

                var overstyringBuilderForPermisjon = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, InternArbeidsforholdRef.ref(internArbeidsforholdId))
                    .medBekreftetPermisjon(new BekreftetPermisjon(permisjonsperiode.getFomDato(), permisjonsperiode.getTomDato(), avklartArbeidsForhold.permisjonStatus()))
                    .medHandling(utledHandlingType(avklartArbeidsForhold.permisjonStatus()));

                arbeidsforholdInformasjonBuilder.leggTil(overstyringBuilderForPermisjon);
                arbeidPermHistorikkInnslagTjeneste.opprettHistorikkinnslag(avklartArbeidsForhold);
            } else {
                throw new IllegalStateException("Ugyldig permisjonsstaus for arbeidsgiverIdent : "+ avklartArbeidsForhold.arbeidsgiverIdent());
            }
        }

        arbeidsforholdAdministrasjonTjeneste.lagreOverstyring(behandlingId, param.getAktørId(), arbeidsforholdInformasjonBuilder );
        return OppdateringResultat.utenTransisjon().build();
    }

    private DatoIntervallEntitet hentPermisjonsperiodeForArbeidsforholdet(AvklarPermisjonUtenSluttdatoDto avklartArbeidsForhold, Yrkesaktivitet yrkesaktiviet) {
        return yrkesaktiviet.getPermisjon().stream()
            .filter( p-> p.getTilOgMed() == null || TIDENES_ENDE.equals(p.getTilOgMed()))
            .map(Permisjon::getPeriode)
            .min(Comparator.naturalOrder())
            .orElseThrow(() -> new IllegalStateException("Finner ikke permisjon uten sluttdato for arbeidsgiverIdent: " + avklartArbeidsForhold.arbeidsgiverIdent()));
    }

    private Yrkesaktivitet hentYrkesaktivietForArbeidsforholdet(AksjonspunktOppdaterParameter param,
                                            InntektArbeidYtelseGrunnlag iayGrunnlag,
                                            AvklarPermisjonUtenSluttdatoDto avklartArbeidsForhold) {
        return new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(param.getAktørId())).getYrkesaktiviteter().stream()
            .filter(ya -> ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator().equals(avklartArbeidsForhold.arbeidsgiverIdent()))
            .filter(ya -> ya.getArbeidsforholdRef().gjelderFor(InternArbeidsforholdRef.ref(avklartArbeidsForhold.internArbeidsforholdId())))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke arbeidsinformasjon for arbeidsgiverIdent: " + avklartArbeidsForhold.arbeidsgiverIdent()));
    }

    private ArbeidsforholdHandlingType utledHandlingType(BekreftetPermisjonStatus permisjonStatus) {
        return BekreftetPermisjonStatus.BRUK_PERMISJON.equals(permisjonStatus) ? ArbeidsforholdHandlingType.IKKE_BRUK : ArbeidsforholdHandlingType.BRUK;
    }

    private boolean erGyldigPermisjonStatus(BekreftetPermisjonStatus permisjonStatus) {
        return BekreftetPermisjonStatus.BRUK_PERMISJON.equals(permisjonStatus)
            || BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON.equals(permisjonStatus);
    }

    private Arbeidsgiver hentArbeidsgiver(String identifikator) {
        return OrgNummer.erGyldigOrgnr(identifikator)
            ? Arbeidsgiver.virksomhet(identifikator)
            : Arbeidsgiver.person(new AktørId(identifikator));

    }
}
