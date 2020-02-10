package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.FinnNavnForManueltLagtTilArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarAktivitetsPerioderDto;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarOpptjeningAktivitetDto;
import no.nav.foreldrepenger.domene.opptjening.dto.BekreftOpptjeningPeriodeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAktivitetsPerioderDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAktivitetsPerioderOppdaterer implements AksjonspunktOppdaterer<AvklarAktivitetsPerioderDto> {

    private static final String CHARS = "a-z0-9_:-";

    private static final String VALID_REGEXP = "^(-?[1-9]|[a-z0])[" + CHARS + "]*$";

    private static final Pattern AKTØRID_VALIDATOR = Pattern.compile(VALID_REGEXP, Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String IKKE_GODKJENT_FOR_PERIODEN = "ikke godkjent for perioden ";
    private static final String GODKJENT_FOR_PERIODEN = "godkjent for perioden ";

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private VirksomhetTjeneste virksomhetTjeneste;
    private PersonIdentTjeneste tpsTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening;

    AvklarAktivitetsPerioderOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAktivitetsPerioderOppdaterer(OpptjeningRepository opptjeningRepository,
                                              InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                              AksjonspunktutlederForVurderOppgittOpptjening vurderOppgittOpptjening,
                                              HistorikkTjenesteAdapter historikkAdapter,
                                              VirksomhetTjeneste virksomhetTjeneste,
                                              PersonIdentTjeneste tpsTjeneste) {
        this.opptjeningRepository = opptjeningRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderOppgittOpptjening = vurderOppgittOpptjening;
        this.historikkAdapter = historikkAdapter;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.tpsTjeneste = tpsTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAktivitetsPerioderDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();
        if (dto.getOpptjeningAktivitetList().stream().anyMatch(oa -> oa.getErGodkjent() == null)) {
            throw new IllegalStateException("AvklarAktivitetsPerioder: Uavklarte aktiviteter til oppdaterer");
        }
        List<ArbeidsforholdOverstyring> overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdInformasjon().map(ArbeidsforholdInformasjon::getOverstyringer).orElse(Collections.emptyList());
        List<BekreftOpptjeningPeriodeDto> bekreftOpptjeningPerioder = map(dto.getOpptjeningAktivitetList(), behandlingId, overstyringer);
        Skjæringstidspunkt skjæringstidspunkt = param.getSkjæringstidspunkt();

        AktørId aktørId = param.getAktørId();
        new BekreftOpptjeningPeriodeAksjonspunkt(inntektArbeidYtelseTjeneste, vurderOppgittOpptjening)
            .oppdater(behandlingId, aktørId, bekreftOpptjeningPerioder, skjæringstidspunkt);

        boolean erEndret = erDetGjortEndringer(dto, behandlingId, overstyringer);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
    }

    private boolean erDetGjortEndringer(AvklarAktivitetsPerioderDto dto, Long behandlingId, List<ArbeidsforholdOverstyring> overstyringer) {
        boolean erEndret = false;
        for (AvklarOpptjeningAktivitetDto oaDto : dto.getOpptjeningAktivitetList()) {
            LocalDateInterval tilVerdi = new LocalDateInterval(oaDto.getOpptjeningFom(), oaDto.getOpptjeningTom());
            if (!oaDto.getErGodkjent()) {
                lagUtfallHistorikk(oaDto, behandlingId, tilVerdi, IKKE_GODKJENT_FOR_PERIODEN, overstyringer);
                erEndret = true;
            } else if (oaDto.getErGodkjent() && oaDto.getErEndret() != null && oaDto.getErEndret()) {
                lagUtfallHistorikk(oaDto, behandlingId, tilVerdi, GODKJENT_FOR_PERIODEN, overstyringer);
                erEndret = true;
            }
        }
        return erEndret;
    }

    private void lagUtfallHistorikk(AvklarOpptjeningAktivitetDto oaDto, Long behandlingId, LocalDateInterval tilVerdi, String godkjentForPerioden, List<ArbeidsforholdOverstyring> overstyringer) {
        Optional<Opptjening> opptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjeningOptional.isPresent()) {
            LocalDateInterval opptjentPeriode = new LocalDateInterval(opptjeningOptional.get().getFom(), opptjeningOptional.get().getTom());
            if (tilVerdi.contains(opptjentPeriode)) {
                byggHistorikkinnslag(behandlingId, oaDto, null, godkjentForPerioden + formaterPeriode(opptjentPeriode),
                    HistorikkinnslagType.FAKTA_ENDRET, HistorikkEndretFeltType.AKTIVITET, overstyringer);
            } else {
                byggHistorikkinnslag(behandlingId, oaDto, null, godkjentForPerioden + formaterPeriode(tilVerdi),
                    HistorikkinnslagType.FAKTA_ENDRET, HistorikkEndretFeltType.AKTIVITET, overstyringer);
            }
            lagEndretHistorikk(behandlingId, oaDto, opptjentPeriode, overstyringer);
        } else {
            byggHistorikkinnslag(behandlingId, oaDto, null, godkjentForPerioden + formaterPeriode(tilVerdi),
                HistorikkinnslagType.FAKTA_ENDRET, HistorikkEndretFeltType.AKTIVITET, overstyringer);
        }
    }

    private void lagEndretHistorikk(Long behandlingId, AvklarOpptjeningAktivitetDto oaDto, LocalDateInterval opptjentPeriode, List<ArbeidsforholdOverstyring> overstyringer) {
        if (erAktivitetEndretForOpptjening(oaDto, opptjentPeriode)) {
            LocalDateInterval fraInterval = new LocalDateInterval(oaDto.getOriginalFom(), oaDto.getOriginalTom());
            LocalDateInterval tilInterval = hentTilInterval(oaDto, opptjentPeriode);
            byggHistorikkinnslag(behandlingId, oaDto, formaterPeriode(fraInterval), formaterPeriode(tilInterval),
                HistorikkinnslagType.FAKTA_ENDRET, HistorikkEndretFeltType.AKTIVITET_PERIODE, overstyringer);
        }
    }

    private LocalDateInterval hentTilInterval(AvklarOpptjeningAktivitetDto oaDto, LocalDateInterval opptjentPeriode) {
        LocalDate fom;
        LocalDate tom;

        if (opptjentPeriode.getTomDato() != null && oaDto.getOpptjeningTom() != null && oaDto.getOpptjeningTom().isEqual(opptjentPeriode.getTomDato())) {
            if (oaDto.getOriginalTom() != null && oaDto.getOriginalTom().isAfter(opptjentPeriode.getTomDato())) {
                tom = oaDto.getOriginalTom();
            } else {
                tom = oaDto.getOpptjeningTom();
            }
        } else {
            tom = oaDto.getOpptjeningTom();
        }

        if (opptjentPeriode.getFomDato() != null && oaDto.getOpptjeningFom() != null && oaDto.getOpptjeningFom().isEqual(opptjentPeriode.getFomDato())) {
            if (oaDto.getOriginalFom() != null && oaDto.getOriginalFom().isBefore(opptjentPeriode.getFomDato())) {
                fom = oaDto.getOriginalFom();
            } else {
                fom = oaDto.getOpptjeningFom();
            }
        } else {
            fom = oaDto.getOpptjeningFom();
        }

        return new LocalDateInterval(fom, tom);
    }

    private void byggHistorikkinnslag(Long behandlingId, AvklarOpptjeningAktivitetDto oaDto, String fraVerdi, String tilVerdi,
                                      HistorikkinnslagType histType, HistorikkEndretFeltType feltType, List<ArbeidsforholdOverstyring> overstyringer) {
        if (OpptjeningAktivitetType.ARBEID.equals(oaDto.getAktivitetType())) {
            lagHistorikkinnslagDel(behandlingId, byggArbeidTekst(oaDto, overstyringer), fraVerdi, tilVerdi, oaDto.getBegrunnelse(), histType, feltType);
        } else {
            lagHistorikkinnslagDel(behandlingId, byggAnnenAktivitetTekst(oaDto), fraVerdi, tilVerdi, oaDto.getBegrunnelse(), histType, feltType);
        }
    }

    private String byggArbeidTekst(AvklarOpptjeningAktivitetDto oaDto, List<ArbeidsforholdOverstyring> overstyringer) {
        String arbeidsgiverIdentifikator = oaDto.getOppdragsgiverOrg() != null ? oaDto.getOppdragsgiverOrg() : oaDto.getArbeidsgiverIdentifikator();
        if (OrganisasjonsNummerValidator.erGyldig(arbeidsgiverIdentifikator)) {
            Virksomhet virksomhet = virksomhetTjeneste.finnOrganisasjon(arbeidsgiverIdentifikator)
                .orElseThrow(IllegalArgumentException::new); // Utvikler feil hvis exception
            String arbeidTypeNavn = oaDto.getAktivitetType().getNavn();
            return String.format("%s for %s (%s)", arbeidTypeNavn, virksomhet.getNavn(), arbeidsgiverIdentifikator);
        } else if (Organisasjonstype.erKunstig(arbeidsgiverIdentifikator)) {
            return hentNavnTilManueltArbeidsforhold(overstyringer);
        } else if (arbeidsgiverIdentifikator != null && AKTØRID_VALIDATOR.matcher(arbeidsgiverIdentifikator).matches()) {
            final Optional<Personinfo> personinfo = tpsTjeneste.hentBrukerForAktør(new AktørId(arbeidsgiverIdentifikator));
            return OpptjeningAktivitetType.ARBEID.getNavn() + " for " + personinfo.map(Personinfo::getNavn).orElse("N/A");
        } else {
            return OpptjeningAktivitetType.ARBEID.getNavn() + " for organisasjonsnr. " + arbeidsgiverIdentifikator;
        }
    }

    private String byggAnnenAktivitetTekst(AvklarOpptjeningAktivitetDto oaDto) {
        return oaDto.getAktivitetType().getNavn();
    }

    private void lagHistorikkinnslagDel(Long behandlingId, String navnVerdi, String fraVerdi, String tilVerdi,
                                        String begrunnelse, HistorikkinnslagType type, HistorikkEndretFeltType feltType) {
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        historikkInnslagTekstBuilder
            .medEndretFelt(feltType, navnVerdi,
                fraVerdi, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_OPPTJENING)
            .medBegrunnelse(begrunnelse);

        historikkAdapter.opprettHistorikkInnslag(behandlingId, type);
    }

    private String formaterPeriode(LocalDateInterval periode) {
        return formatDate(periode.getFomDato()) + " - " + formatDate(periode.getTomDato());
    }

    private String formatDate(LocalDate localDate) {
        if (Tid.TIDENES_ENDE.equals(localDate)) {
            return "d.d.";
        }
        return DATE_FORMATTER.format(localDate);
    }

    private boolean erAktivitetEndretForOpptjening(AvklarOpptjeningAktivitetDto oaDto, LocalDateInterval opptjentPeriode) {
        boolean aktivitetEndret = false;

        if (erLocalDateGyldigOgEndret(opptjentPeriode.getTomDato(), oaDto.getOriginalTom(), oaDto.getOpptjeningTom())
            || erLocalDateGyldigOgEndret(opptjentPeriode.getFomDato(), oaDto.getOriginalFom(), oaDto.getOpptjeningFom())) {
            if (!oaDto.getOriginalFom().equals(oaDto.getOpptjeningFom()) || !oaDto.getOriginalTom().equals(oaDto.getOpptjeningTom())) {
                aktivitetEndret = true;
            }
        }

        return aktivitetEndret;
    }

    private boolean erLocalDateGyldigOgEndret(LocalDate opptjentPeriode, LocalDate dtoOriginal, LocalDate dtoOpptjening) {
        return opptjentPeriode != null && dtoOriginal != null && dtoOpptjening != null && !dtoOpptjening.isEqual(opptjentPeriode);
    }

    private List<BekreftOpptjeningPeriodeDto> map(List<AvklarOpptjeningAktivitetDto> opptjeningAktiviteter, Long behandlingId, List<ArbeidsforholdOverstyring> overstyringer) {
        List<BekreftOpptjeningPeriodeDto> list = new ArrayList<>();
        Opptjening opptjening = opptjeningRepository.finnOpptjening(behandlingId)
            .orElseThrow(IllegalArgumentException::new);
        opptjeningAktiviteter.forEach(aktivitet -> {
            BekreftOpptjeningPeriodeDto adapter = new BekreftOpptjeningPeriodeDto();
            adapter.setAktivitetType(aktivitet.getAktivitetType());
            adapter.setOriginalFom(aktivitet.getOriginalFom());
            adapter.setOriginalTom(aktivitet.getOriginalTom());

            if (OpptjeningAktivitetType.ARBEID.equals(aktivitet.getAktivitetType())) {
                mapOpptjeningAktivitetTypeARBEID(aktivitet, adapter, overstyringer);
            }
            String arbeidsgiverIdentifikator = aktivitet.getOppdragsgiverOrg() != null ? aktivitet.getOppdragsgiverOrg() : aktivitet.getArbeidsgiverIdentifikator();
            adapter.setArbeidsgiverIdentifikator(arbeidsgiverIdentifikator);
            adapter.setStillingsandel(aktivitet.getStillingsandel());
            adapter.setNaringRegistreringsdato(aktivitet.getNaringRegistreringsdato());
            adapter.setErManueltOpprettet(aktivitet.getErManueltOpprettet());
            adapter.setErGodkjent(aktivitet.getErGodkjent());
            boolean erEndret = erEndret(
                DatoIntervallEntitet.fraOgMedTilOgMed(opptjening.getFom(), opptjening.getTom()),
                DatoIntervallEntitet.fraOgMedTilOgMed(aktivitet.getOpptjeningFom(), aktivitet.getOpptjeningTom()), aktivitet.getOriginalFom() != null
                    ? DatoIntervallEntitet.fraOgMedTilOgMed(aktivitet.getOriginalFom(), aktivitet.getOriginalTom()) : null);
            settPeriode(opptjening, aktivitet, adapter, erEndret);
            adapter.setErEndret(erEndret);

            adapter.setBegrunnelse(aktivitet.getBegrunnelse());
            adapter.setArbeidsforholdRef(aktivitet.getArbeidsforholdRef());
            list.add(adapter);
        });
        return list;
    }

    private void mapOpptjeningAktivitetTypeARBEID(AvklarOpptjeningAktivitetDto l, BekreftOpptjeningPeriodeDto adapter, List<ArbeidsforholdOverstyring> overstyringer) {
        String arbeidsgiverIdentifikator = l.getOppdragsgiverOrg() != null ? l.getOppdragsgiverOrg() : l.getArbeidsgiverIdentifikator();
        if (OrganisasjonsNummerValidator.erGyldig(arbeidsgiverIdentifikator)) {
            Virksomhet virksomhet = virksomhetTjeneste.finnOrganisasjon(arbeidsgiverIdentifikator)
                .orElseThrow(IllegalArgumentException::new); // Utvikler feil hvis exception
            if (virksomhet.getNavn() != null) {
                adapter.setArbeidsgiverNavn(virksomhet.getNavn());
            }
        } else if (Organisasjonstype.erKunstig(arbeidsgiverIdentifikator)) {
            adapter.setArbeidsgiverNavn(hentNavnTilManueltArbeidsforhold(overstyringer));
        } else if (arbeidsgiverIdentifikator != null && AKTØRID_VALIDATOR.matcher(arbeidsgiverIdentifikator).matches()) {
            final Optional<Personinfo> personinfo = tpsTjeneste.hentBrukerForAktør(new AktørId(arbeidsgiverIdentifikator));
            adapter.setArbeidsgiverNavn(personinfo.map(Personinfo::getNavn).orElse("N/A"));
        } else {
            adapter.setArbeidsgiverNavn("N/A");
        }
    }

    private void settPeriode(Opptjening opptjening, AvklarOpptjeningAktivitetDto aktivitet, BekreftOpptjeningPeriodeDto adapter, boolean erEndret) {
        if (erEndret) {
            oppdaterFom(opptjening, aktivitet, adapter);
            oppdaterTom(opptjening, aktivitet, adapter);
        } else {
            adapter.setOpptjeningFom(aktivitet.getOriginalFom());
            adapter.setOpptjeningTom(aktivitet.getOriginalTom());
        }
    }

    private void oppdaterTom(Opptjening opptjening, AvklarOpptjeningAktivitetDto aktivitet, BekreftOpptjeningPeriodeDto adapter) {
        if (aktivitet.getOpptjeningTom().equals(opptjening.getTom())) {
            if (aktivitet.getOriginalTom() != null && aktivitet.getOriginalTom().isAfter(aktivitet.getOpptjeningTom())) {
                adapter.setOpptjeningTom(aktivitet.getOriginalTom() != null ? aktivitet.getOriginalTom() : aktivitet.getOpptjeningTom());
            } else {
                adapter.setOpptjeningTom(aktivitet.getOpptjeningTom());
            }
        } else {
            adapter.setOpptjeningTom(aktivitet.getOpptjeningTom());
        }
    }

    private void oppdaterFom(Opptjening opptjening, AvklarOpptjeningAktivitetDto aktivitet, BekreftOpptjeningPeriodeDto adapter) {
        if (aktivitet.getOpptjeningFom().equals(opptjening.getFom())) {
            if (aktivitet.getOriginalFom() != null && aktivitet.getOriginalFom().isBefore(aktivitet.getOpptjeningFom())) {
                adapter.setOpptjeningFom(aktivitet.getOriginalFom() != null ? aktivitet.getOriginalFom() : aktivitet.getOpptjeningFom());
            } else {
                adapter.setOpptjeningFom(aktivitet.getOpptjeningFom());
            }
        } else {
            adapter.setOpptjeningFom(aktivitet.getOpptjeningFom());
        }
    }

    boolean erEndret(DatoIntervallEntitet beregnetOpptjening, DatoIntervallEntitet aktivitetPeriode, DatoIntervallEntitet orginalPeriode) {
        if (orginalPeriode == null) {
            return true;
        }
        if (orginalPeriode.inkluderer(beregnetOpptjening.getFomDato()) && orginalPeriode.inkluderer(beregnetOpptjening.getTomDato())) {
            return !beregnetOpptjening.equals(aktivitetPeriode);
        } else if (beregnetOpptjening.inkluderer(orginalPeriode.getTomDato()) && beregnetOpptjening.inkluderer(orginalPeriode.getFomDato())) {
            return !orginalPeriode.equals(aktivitetPeriode);
        } else if (beregnetOpptjening.inkluderer(orginalPeriode.getTomDato()) && !beregnetOpptjening.inkluderer(orginalPeriode.getFomDato())) {
            return !DatoIntervallEntitet.fraOgMedTilOgMed(beregnetOpptjening.getFomDato(), orginalPeriode.getTomDato()).equals(aktivitetPeriode);
        }
        return !DatoIntervallEntitet.fraOgMedTilOgMed(orginalPeriode.getFomDato(), beregnetOpptjening.getTomDato()).equals(aktivitetPeriode);
    }

    private String hentNavnTilManueltArbeidsforhold(List<ArbeidsforholdOverstyring> overstyringer) {
        return FinnNavnForManueltLagtTilArbeidsforholdTjeneste.finnNavnTilManueltLagtTilArbeidsforhold(overstyringer)
            .map(ArbeidsgiverOpplysninger::getNavn)
            .orElseThrow(() -> new IllegalStateException("Fant ikke forventet informasjon om manuelt arbeidsforhold"));
    }
}
