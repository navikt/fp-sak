package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.dto.BekreftOpptjeningPeriodeDto;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

class BekreftOpptjeningPeriodeAksjonspunkt {
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private AksjonspunktutlederForVurderOppgittOpptjening vurderOpptjening;

    BekreftOpptjeningPeriodeAksjonspunkt(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
            AksjonspunktutlederForVurderOppgittOpptjening vurderOpptjening) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderOpptjening = vurderOpptjening;
    }

    void oppdater(Long behandlingId, AktørId aktørId, Collection<BekreftOpptjeningPeriodeDto> bekreftOpptjeningPerioder,
            Skjæringstidspunkt skjæringstidspunkt) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);

        var builder = inntektArbeidYtelseTjeneste.opprettBuilderForSaksbehandlet(behandlingId);
        var overstyrtBuilder = builder.getAktørArbeidBuilder(aktørId);

        var kodeRelasjonMap = OpptjeningAktivitetType.hentTilArbeidTypeRelasjoner();

        var bekreftetOverstyrtPeriode = bekreftOpptjeningPerioder.stream()
                .filter(it -> kanOverstyresOgSkalKunneLagreResultat(behandlingId, aktørId, iayGrunnlag, kodeRelasjonMap, it, skjæringstidspunkt))
                .collect(Collectors.toList());

        for (var periode : bekreftetOverstyrtPeriode) {
            var yrkesaktivitetBuilder = getYrkesaktivitetBuilder(behandlingId, aktørId, iayGrunnlag, overstyrtBuilder, periode,
                    kodeRelasjonMap.get(periode.getAktivitetType()));
            if (periode.getErGodkjent()) {
                var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder
                        .getAktivitetsAvtaleBuilder(getOrginalPeriode(periode), true);

                håndterPeriodeForAnnenOpptjening(periode, yrkesaktivitetBuilder, aktivitetsAvtaleBuilder);
                aktivitetsAvtaleBuilder.medBeskrivelse(periode.getBegrunnelse());
                yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);
                overstyrtBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
            } else {
                yrkesaktivitetBuilder.fjernPeriode(getOrginalPeriode(periode));
                if (yrkesaktivitetBuilder.harIngenAvtaler() && yrkesaktivitetBuilder.getErOppdatering()) {
                    // Finnes perioden i builder så skal den fjernes.
                    overstyrtBuilder.fjernYrkesaktivitetHvisFinnes(yrkesaktivitetBuilder);
                }
            }
        }
        builder.leggTilAktørArbeid(overstyrtBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingId, builder);
    }

    private void håndterPeriodeForAnnenOpptjening(BekreftOpptjeningPeriodeDto periode, YrkesaktivitetBuilder yrkesaktivitetBuilder,
            AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder) {
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(periode.getAktivitetType())
                || periode.getAktivitetType().equals(OpptjeningAktivitetType.NÆRING)) {
            if (periode.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)) {
                settArbeidsgiverInformasjon(periode.getArbeidsgiverNavn(), periode.getArbeidsgiverIdentifikator(), yrkesaktivitetBuilder);
            }
            final DatoIntervallEntitet aktivitetsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getOpptjeningFom(),
                    periode.getOpptjeningTom());
            aktivitetsAvtaleBuilder.medPeriode(aktivitetsPeriode);
        }
        if (periode.getErEndret()) {
            final DatoIntervallEntitet aktivitetsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getOpptjeningFom(),
                    periode.getOpptjeningTom());
            aktivitetsAvtaleBuilder.medPeriode(aktivitetsPeriode);
        }
    }

    private void settArbeidsgiverInformasjon(String arbeidsgiver, String oppdragsgiverOrg, YrkesaktivitetBuilder yrkesaktivitetBuilder) {
        if (arbeidsgiver != null) {
            yrkesaktivitetBuilder.medArbeidsgiverNavn(arbeidsgiver);
        }
        if (oppdragsgiverOrg != null) {
            yrkesaktivitetBuilder.medArbeidsgiver(Arbeidsgiver.virksomhet(oppdragsgiverOrg));
        }
    }

    private boolean kanOverstyresOgSkalKunneLagreResultat(Long behandlingId, AktørId aktørId, Optional<InntektArbeidYtelseGrunnlag> iayg,
            Map<OpptjeningAktivitetType, Set<ArbeidType>> kodeRelasjonMap, BekreftOpptjeningPeriodeDto periode,
            Skjæringstidspunkt skjæringstidspunkt) {
        if (!kodeRelasjonMap.containsKey(periode.getAktivitetType())) {
            return false;
        }
        final Set<ArbeidType> arbeidTypes = kodeRelasjonMap.get(periode.getAktivitetType());
        return kanSaksbehandles(behandlingId, aktørId, iayg, arbeidTypes, periode, skjæringstidspunkt);
    }

    private boolean kanSaksbehandles(Long behandlingId, AktørId aktørId, Optional<InntektArbeidYtelseGrunnlag> iaygOpt, Set<ArbeidType> arbeidTypes,
            BekreftOpptjeningPeriodeDto periode, Skjæringstidspunkt skjæringstidspunkt) {
        if (OpptjeningAktivitetType.ARBEID.equals(periode.getAktivitetType())) {
            if (!iaygOpt.isPresent()) {
                return false;
            }
            var iayg = iaygOpt.get();
            var filter = new YrkesaktivitetFilter(iayg.getArbeidsforholdInformasjon(), iayg.getAktørArbeidFraRegister(aktørId))
                    .før(skjæringstidspunkt.getUtledetSkjæringstidspunkt());
            return harGittAksjonspunktForArbeidsforhold(filter, arbeidTypes, periode);
        } else if (OpptjeningAktivitetType.NÆRING.equals(periode.getAktivitetType())) {
            if (!iaygOpt.isPresent()) {
                return false;
            }
            var iayg = iaygOpt.get();
            return harGittAksjonspunktForNæring(behandlingId, aktørId, iayg, skjæringstidspunkt);
        }
        return OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(periode.getAktivitetType());
    }

    private boolean harGittAksjonspunktForArbeidsforhold(YrkesaktivitetFilter filter, Set<ArbeidType> arbeidTypes,
            BekreftOpptjeningPeriodeDto periode) {
        var yrkesaktiviteter = filter.getYrkesaktiviteter();
        if (yrkesaktiviteter.isEmpty()) {
            return false;
        }
        // Sjekk om 0%
        return yrkesaktiviteter
                .stream()
                .anyMatch(it -> {
                    boolean erNullProsent = filter.getAktivitetsAvtalerForArbeid(it).stream()
                            .anyMatch(aa -> (aa.getProsentsats() == null) || aa.getProsentsats().erNulltall());
                    boolean erKunstig = it.getArbeidsgiver().getErVirksomhet() && Organisasjonstype.erKunstig(it.getArbeidsgiver().getOrgnr());
                    return arbeidTypes.contains(it.getArbeidType())
                            && it.getArbeidsgiver().getIdentifikator().equals(periode.getArbeidsgiverIdentifikator())
                            && (erNullProsent
                                    || erKunstig);
                });
    }

    private boolean harGittAksjonspunktForNæring(Long behandlingId, AktørId aktørId, InntektArbeidYtelseGrunnlag iayg,
            Skjæringstidspunkt skjæringstidspunkt) {
        return vurderOpptjening.girAksjonspunktForOppgittNæring(behandlingId, aktørId, iayg, skjæringstidspunkt);
    }

    private DatoIntervallEntitet getOrginalPeriode(BekreftOpptjeningPeriodeDto periode) {
        if (periode.getErManueltOpprettet()) {
            final LocalDate tomDato = periode.getOpptjeningTom() != null ? periode.getOpptjeningTom() : Tid.TIDENES_ENDE;
            return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getOpptjeningFom(), tomDato);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getOriginalFom(), periode.getOriginalTom());
    }

    private YrkesaktivitetBuilder getYrkesaktivitetBuilder(Long behandlingId, AktørId aktørId,
            Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag,
            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder overstyrtBuilder,
            BekreftOpptjeningPeriodeDto periodeDto,
            Set<ArbeidType> arbeidType) {
        if ((arbeidType == null) || arbeidType.isEmpty()) {
            throw new IllegalStateException("Støtter ikke " + periodeDto.getAktivitetType().getKode());
        }
        YrkesaktivitetBuilder builder;
        if (periodeDto.getAktivitetType().equals(OpptjeningAktivitetType.ARBEID)) {
            Opptjeningsnøkkel nøkkel;
            String id = periodeDto.getArbeidsforholdRef();
            InternArbeidsforholdRef ref = id == null ? null : InternArbeidsforholdRef.ref(id);
            if (OrganisasjonsNummerValidator.erGyldig(periodeDto.getArbeidsgiverIdentifikator())
                    || Organisasjonstype.erKunstig(periodeDto.getArbeidsgiverIdentifikator())) {
                nøkkel = new Opptjeningsnøkkel(ref, periodeDto.getArbeidsgiverIdentifikator(), null);
            } else {
                nøkkel = new Opptjeningsnøkkel(ref, null, periodeDto.getArbeidsgiverIdentifikator());
            }
            builder = overstyrtBuilder.getYrkesaktivitetBuilderForNøkkelAvType(nøkkel, arbeidType);

            if (!builder.getErOppdatering()) {
                if (Organisasjonstype.erKunstig(periodeDto.getArbeidsgiverIdentifikator())) {
                    builder = kopierVerdierForFiktivtArbeidsforhold(iayGrunnlag, builder);
                } else {
                    // Bør få med all informasjon om arbeidsforholdet over i overstyrt slik at
                    // ingenting blir mistet.
                    builder = getRegisterBuilder(behandlingId, aktørId).getYrkesaktivitetBuilderForNøkkelAvType(nøkkel, arbeidType)
                            .migrerFraRegisterTilOverstyrt();
                }
            }
            builder.medArbeidsforholdId(ref);
        } else {
            builder = overstyrtBuilder.getYrkesaktivitetBuilderForType(arbeidType.stream().findFirst().orElse(ArbeidType.UDEFINERT));
        }
        return builder;
    }

    private YrkesaktivitetBuilder kopierVerdierForFiktivtArbeidsforhold(Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag,
            YrkesaktivitetBuilder builder) {
        Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjon = iayGrunnlag
                .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
        if (arbeidsforholdInformasjon.isPresent()) {
            // Det finnes kun en overstyring om vi har et fiktivt arbeidsforhold
            List<ArbeidsforholdOverstyring> overstyringer = arbeidsforholdInformasjon.get().getOverstyringer();
            if (overstyringer.size() != 1) {
                throw new IllegalStateException("Fant ingen eller mer enn en overstyring for fiktivt arbeidsforhold");
            }
            ArbeidsforholdOverstyring fiktivOverstyring = overstyringer.get(0);
            builder.medArbeidsgiver(fiktivOverstyring.getArbeidsgiver());
            builder.medArbeidsgiverNavn(fiktivOverstyring.getArbeidsgiverNavn());
        }
        return builder;
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder getRegisterBuilder(Long behandlingId, AktørId aktørId) {
        return inntektArbeidYtelseTjeneste.opprettBuilderForRegister(behandlingId).getAktørArbeidBuilder(aktørId);
    }
}
