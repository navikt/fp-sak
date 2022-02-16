package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftArbeidMedPermisjonUtenSluttdatoDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarArbeidPermisjonUtenSluttdatoOppdaterer implements AksjonspunktOppdaterer<BekreftArbeidMedPermisjonUtenSluttdatoDto> {
    @Valid
    @Size(max = 1000)
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private ArbeidsforholdHistorikkTjeneste arbeidsforholdHistorikkTjeneste;

    AvklarArbeidPermisjonUtenSluttdatoOppdaterer(){
        //CDI
    }


    public AvklarArbeidPermisjonUtenSluttdatoOppdaterer(ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                                        ArbeidsforholdHistorikkTjeneste arbeidsforholdHistorikkTjeneste) {
        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.arbeidsforholdHistorikkTjeneste = arbeidsforholdHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftArbeidMedPermisjonUtenSluttdatoDto bekreftetArbforholdDto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var arbeidsforholdInformasjonBuilder = arbeidsforholdAdministrasjonTjeneste.opprettBuilderFor(behandlingId).tilbakestillOverstyringer();

        for (var avklartArbeidsForhold : bekreftetArbforholdDto.getArbeidsforhold()) {
            if (erGyldigPermisjonStatus(avklartArbeidsForhold.permisjonStatus())) {
                var internArbeidsforholdId = avklartArbeidsForhold.internArbeidsforholdId();
                var arbeidsgiver = hentArbeidsgiver(avklartArbeidsForhold.arbeidsgiverIdent());
                var internArbeidsforholdRef = InternArbeidsforholdRef.ref(internArbeidsforholdId);
                var overstyringBuilderFor = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, InternArbeidsforholdRef.ref(internArbeidsforholdId))
                    .medBekreftetPermisjon(new BekreftetPermisjon(avklartArbeidsForhold.permisjonStatus()));

                arbeidsforholdInformasjonBuilder.leggTil(overstyringBuilderFor);
                arbeidsforholdHistorikkTjeneste.opprettHistorikkinnslag(arbeidsgiver, internArbeidsforholdRef, avklartArbeidsForhold);
            } else {
                throw new IllegalStateException("Ugyldig permisjonsstaus for arbeidsgiverIdent : "+ avklartArbeidsForhold.arbeidsgiverIdent());
            }
        }

        arbeidsforholdAdministrasjonTjeneste.lagreOverstyring(behandlingId, param.getAktørId(), arbeidsforholdInformasjonBuilder );
        return OppdateringResultat.utenTransisjon().build();
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
