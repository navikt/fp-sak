package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarAktivitetsPerioderDto;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarOpptjeningAktivitetDto;
import no.nav.foreldrepenger.domene.opptjening.dto.BekreftOpptjeningPeriodeDto;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAktivitetsPerioderDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktivitetsPerioderOppdaterer implements AksjonspunktOppdaterer<AvklarAktivitetsPerioderDto> {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String IKKE_GODKJENT_FOR_PERIODEN = "ikke godkjent for perioden ";
    private static final String GODKJENT_FOR_PERIODEN = "godkjent for perioden ";

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Historikkinnslag2Repository historikkinnslagRepository;
    private AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening;
    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    AvklarAktivitetsPerioderOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktivitetsPerioderOppdaterer(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                              AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening,
                                              Historikkinnslag2Repository historikkinnslagRepository,
                                              OpptjeningsperioderTjeneste opptjeningsperioderTjeneste,
                                              ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderOppgittOpptjening = vurderOppgittOpptjening;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAktivitetsPerioderDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        if (dto.getOpptjeningsaktiviteter().stream().anyMatch(oa -> oa.getErGodkjent() == null)) {
            throw new IllegalStateException("AvklarAktivitetsPerioder: Uavklarte aktiviteter til oppdaterer");
        }
        var overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getOverstyringer).orElse(Collections.emptyList());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var opptjeningsaktiviteter = opptjeningsperioderTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(param.getRef(), skjæringstidspunkt);
        var bekreftOpptjeningPerioder = map(dto.getOpptjeningsaktiviteter(), overstyringer);


        var aktørId = param.getAktørId();
        new BekreftOpptjeningPeriodeAksjonspunkt(inntektArbeidYtelseTjeneste, vurderOppgittOpptjening)
                .oppdater(behandlingId, aktørId, bekreftOpptjeningPerioder, skjæringstidspunkt);

        if (!bekreftOpptjeningPerioder.isEmpty()) { // erEndret tok ikke hensyn til om disse var like de gamle
            var fagsakId = param.getRef().fagsakId();
            for (var bekreftetAktivitet : bekreftOpptjeningPerioder) {
                var aktivitetNavn = OpptjeningAktivitetType.ARBEID.equals(bekreftetAktivitet.getAktivitetType())
                    ? bekreftetAktivitet.getArbeidsgiverNavn()
                    : bekreftetAktivitet.getAktivitetType().getNavn();
                var eksisterendeAktivitet = finnLagretAktivitet(bekreftetAktivitet, opptjeningsaktiviteter);
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(bekreftetAktivitet.getOpptjeningFom(), bekreftetAktivitet.getOpptjeningTom());
                var fraVerdi = finnFraVerdi(eksisterendeAktivitet, periode);
                var tilVerdi = finnTilVerdi(bekreftetAktivitet, periode);
                var historikkinnslag = new Historikkinnslag2.Builder()
                    .medAktør(HistorikkAktør.SAKSBEHANDLER)
                    .medFagsakId(fagsakId)
                    .medBehandlingId(behandlingId)
                    .medTittel(SkjermlenkeType.FAKTA_OM_OPPTJENING)
                    .addTekstlinje(fraTilEquals(String.format("Aktivitet %s", aktivitetNavn), fraVerdi, tilVerdi))
                    .addTekstlinje(bekreftetAktivitet.getBegrunnelse())
                    .build();
                historikkinnslagRepository.lagre(historikkinnslag);
            }
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(true).build();
        } else {
            return OppdateringResultat.utenTransisjon().build();
        }
    }

    private String finnTilVerdi(BekreftOpptjeningPeriodeDto bekreftetAktivitet, DatoIntervallEntitet periode) {
        if (Boolean.TRUE.equals(bekreftetAktivitet.getErGodkjent())) {
            return GODKJENT_FOR_PERIODEN + formaterPeriode(periode);
        }
        return IKKE_GODKJENT_FOR_PERIODEN + formaterPeriode(periode);
    }

    private String finnFraVerdi(OpptjeningsperiodeForSaksbehandling eksisterendeAktivitet, DatoIntervallEntitet periode) {
        if (eksisterendeAktivitet.getVurderingsStatus() == null) {
            return null;
        } else  if (eksisterendeAktivitet.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)) {
            return GODKJENT_FOR_PERIODEN + formaterPeriode(periode);
        } else if (eksisterendeAktivitet.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)) {
            return IKKE_GODKJENT_FOR_PERIODEN + formaterPeriode(periode);
        }
        return null;
    }

    private OpptjeningsperiodeForSaksbehandling finnLagretAktivitet(BekreftOpptjeningPeriodeDto bekreftetAktivitet, List<OpptjeningsperiodeForSaksbehandling> eksisterendeAktiviteter) {
        var eksisterendeLagretAktivitet = eksisterendeAktiviteter.stream().filter(akt -> akt.getOpptjeningAktivitetType().equals(bekreftetAktivitet.getAktivitetType())
            && matcherArbeidsgiverOgRef(akt, bekreftetAktivitet) && erPeriodeLik(bekreftetAktivitet, akt)).findFirst();
        if (eksisterendeLagretAktivitet.isEmpty()) {
            throw new IllegalStateException("Finner ikke matchende lagret aktivitet for bekreftet aktivitet " + bekreftetAktivitet);
        }
        return eksisterendeLagretAktivitet.get();
    }

    private boolean erPeriodeLik(BekreftOpptjeningPeriodeDto bekreftetAktivitet, OpptjeningsperiodeForSaksbehandling akt) {
        return Objects.equals(akt.getPeriode().getFomDato(), bekreftetAktivitet.getOpptjeningFom())
            && Objects.equals(akt.getPeriode().getTomDato(), bekreftetAktivitet.getOpptjeningTom());
    }

    private boolean matcherArbeidsgiverOgRef(OpptjeningsperiodeForSaksbehandling akt, BekreftOpptjeningPeriodeDto bekreftetAktivitet) {
        var lagretArbeidsforholdId = akt.getOpptjeningsnøkkel() == null
            ? null
            : akt.getOpptjeningsnøkkel().getArbeidsforholdRef().map(InternArbeidsforholdRef::getReferanse).orElse(null);
        var lagretArbeidsgiverId = akt.getArbeidsgiver() == null
            ? null
            : akt.getArbeidsgiver().getIdentifikator();

        var bekreftetArbeidsforholdId = bekreftetAktivitet.getArbeidsforholdRef();

        // I tilfeller med opptjening i utlandet kan det mangle et en identifikator for arbeidsgiver i lagret entitet,
        // men den vil være satt i GUI for å gjøre det mulig å matche aktivitet med beskrivende navn.
        // Må derfor sjekke om det er tilfellet her.
        if (lagretArbeidsgiverId == null && ikkeGyldigArbeidsgiverReferanse(bekreftetAktivitet)) {
            return Objects.equals(lagretArbeidsforholdId, bekreftetArbeidsforholdId);

        }
        var bekreftetArbeidsgiverId = bekreftetAktivitet.getArbeidsgiverReferanse();
        return Objects.equals(lagretArbeidsforholdId, bekreftetArbeidsforholdId) && Objects.equals(lagretArbeidsgiverId, bekreftetArbeidsgiverId);
    }

    private boolean ikkeGyldigArbeidsgiverReferanse(BekreftOpptjeningPeriodeDto bekreftetAktivitet) {
        return bekreftetAktivitet.getArbeidsgiverReferanse() == null
            || !OrgNummer.erGyldigOrgnr(bekreftetAktivitet.getArbeidsgiverReferanse()) && !AktørId.erGyldigAktørId(
            bekreftetAktivitet.getArbeidsgiverReferanse());
    }

    private String formaterPeriode(DatoIntervallEntitet periode) {
        return formatDate(periode.getFomDato()) + " - " + formatDate(periode.getTomDato());
    }

    private String formatDate(LocalDate localDate) {
        if (Tid.TIDENES_ENDE.equals(localDate)) {
            return "d.d.";
        }
        return DATE_FORMATTER.format(localDate);
    }

    private List<BekreftOpptjeningPeriodeDto> map(List<AvklarOpptjeningAktivitetDto> opptjeningAktiviteter, List<ArbeidsforholdOverstyring> overstyringer) {
        List<BekreftOpptjeningPeriodeDto> list = new ArrayList<>();
        opptjeningAktiviteter.forEach(aktivitet -> {
            var adapter = new BekreftOpptjeningPeriodeDto();
            adapter.setAktivitetType(aktivitet.getAktivitetType());
            if (OpptjeningAktivitetType.ARBEID.equals(aktivitet.getAktivitetType())) {
                adapter.setArbeidsgiverNavn(finnArbeidsgivernavn(aktivitet, overstyringer));
            }
            adapter.setArbeidsgiverReferanse(aktivitet.getArbeidsgiverReferanse());
            adapter.setErGodkjent(aktivitet.getErGodkjent());
            adapter.setOpptjeningFom(aktivitet.getOpptjeningFom());
            adapter.setOpptjeningTom(aktivitet.getOpptjeningTom());
            adapter.setBegrunnelse(aktivitet.getBegrunnelse());
            adapter.setArbeidsforholdRef(aktivitet.getArbeidsforholdRef());
            list.add(adapter);
        });
        return list;
    }

    private String finnArbeidsgivernavn(AvklarOpptjeningAktivitetDto bekreftetAktivitet, List<ArbeidsforholdOverstyring> overstyringer) {
        var arbeidsgiverIdentifikator = bekreftetAktivitet.getArbeidsgiverReferanse();
        var erKunstigOrgnr = OrgNummer.erKunstig(arbeidsgiverIdentifikator);
        if (erKunstigOrgnr) {
            return hentNavnTilManueltArbeidsforhold(overstyringer);
        }
        var erVanligOrgnr = OrganisasjonsNummerValidator.erGyldig(arbeidsgiverIdentifikator);
        var arbeidsgiver = erVanligOrgnr ? Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator) : Arbeidsgiver.person(new AktørId(arbeidsgiverIdentifikator));
        var opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        if (opplysninger != null) {
            return opplysninger.getNavn();
        } else {
            return "N/A";
        }
    }

    private String hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer) {
        return FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer)
            .map(ArbeidsgiverOpplysninger::getNavn)
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet informasjon om manuelt arbeidsforhold"));
    }
}
