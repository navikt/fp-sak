package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.dto.BekreftOpptjeningPeriodeDto;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class BekreftOpptjeningPeriodeAksjonspunkt {
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private final AksjonspunktutlederForVurderOppgittOpptjening vurderOpptjening;

    BekreftOpptjeningPeriodeAksjonspunkt(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                         AksjonspunktutlederForVurderOppgittOpptjening vurderOpptjening) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vurderOpptjening = vurderOpptjening;
    }

    void oppdater(Long behandlingId,
                  AktørId aktørId,
                  Collection<BekreftOpptjeningPeriodeDto> bekreftedeOpptjeningsaktiviteter,
                  Skjæringstidspunkt skjæringstidspunkt) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);

        var builder = inntektArbeidYtelseTjeneste.opprettBuilderForSaksbehandlet(behandlingId);
        var overstyrtBuilder = builder.getAktørArbeidBuilder(aktørId);

        var kodeRelasjonMap = OpptjeningAktivitetType.hentTilArbeidTypeRelasjoner();

        var ugyldigePerioder = bekreftedeOpptjeningsaktiviteter.stream()
            .filter(it -> !kanOverstyresOgSkalKunneLagreResultat(behandlingId, aktørId, iayGrunnlag, kodeRelasjonMap, it, skjæringstidspunkt))
            .toList();

        if (!ugyldigePerioder.isEmpty()) {
            throw new IllegalStateException(
                "Det finnes saksbehandlede perioder som ikke er åpne for saksbehandling. Ugyldige perioder: " + ugyldigePerioder);
        }

        for (var periode : bekreftedeOpptjeningsaktiviteter) {
            var yrkesaktivitetBuilder = getYrkesaktivitetBuilder(behandlingId, aktørId, iayGrunnlag, overstyrtBuilder, periode,
                kodeRelasjonMap.get(periode.getAktivitetType()));
            if (periode.getErGodkjent()) {
                var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(getPeriode(periode), true);

                håndterPeriodeForAnnenOpptjening(periode, yrkesaktivitetBuilder, aktivitetsAvtaleBuilder);
                aktivitetsAvtaleBuilder.medBeskrivelse(periode.getBegrunnelse());
                yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);
                overstyrtBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
            } else {
                yrkesaktivitetBuilder.fjernPeriode(getPeriode(periode));
                if (yrkesaktivitetBuilder.harIngenAvtaler() && yrkesaktivitetBuilder.getErOppdatering()) {
                    // Finnes perioden i builder så skal den fjernes.
                    overstyrtBuilder.fjernYrkesaktivitetHvisFinnes(yrkesaktivitetBuilder);
                }
            }
        }
        builder.leggTilAktørArbeid(overstyrtBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingId, builder);
    }

    private void håndterPeriodeForAnnenOpptjening(BekreftOpptjeningPeriodeDto periode,
                                                  YrkesaktivitetBuilder yrkesaktivitetBuilder,
                                                  AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder) {
        if (OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(periode.getAktivitetType()) || periode.getAktivitetType()
            .equals(OpptjeningAktivitetType.NÆRING)) {
            if (periode.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD) && periode.getArbeidsgiverNavn() != null) {
                yrkesaktivitetBuilder.medArbeidsgiverNavn(periode.getArbeidsgiverNavn());
            }
            var aktivitetsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getOpptjeningFom(), periode.getOpptjeningTom());
            aktivitetsAvtaleBuilder.medPeriode(aktivitetsPeriode);
        }
    }

    private boolean kanOverstyresOgSkalKunneLagreResultat(Long behandlingId,
                                                          AktørId aktørId,
                                                          Optional<InntektArbeidYtelseGrunnlag> iayg,
                                                          Map<OpptjeningAktivitetType, Set<ArbeidType>> kodeRelasjonMap,
                                                          BekreftOpptjeningPeriodeDto periode,
                                                          Skjæringstidspunkt skjæringstidspunkt) {
        if (!kodeRelasjonMap.containsKey(periode.getAktivitetType())) {
            return false;
        }
        var arbeidTypes = kodeRelasjonMap.get(periode.getAktivitetType());
        return kanSaksbehandles(behandlingId, aktørId, iayg, arbeidTypes, periode, skjæringstidspunkt);
    }

    private boolean kanSaksbehandles(Long behandlingId,
                                     AktørId aktørId,
                                     Optional<InntektArbeidYtelseGrunnlag> iaygOpt,
                                     Set<ArbeidType> arbeidTypes,
                                     BekreftOpptjeningPeriodeDto periode,
                                     Skjæringstidspunkt skjæringstidspunkt) {
        if (OpptjeningAktivitetType.ARBEID.equals(periode.getAktivitetType())) {
            if (iaygOpt.isEmpty()) {
                return false;
            }
            var iayg = iaygOpt.get();
            var filter = new YrkesaktivitetFilter(iayg.getArbeidsforholdInformasjon(), iayg.getAktørArbeidFraRegister(aktørId)).før(
                skjæringstidspunkt.getUtledetSkjæringstidspunkt());
            return harGittAksjonspunktForArbeidsforhold(filter, arbeidTypes, periode);
        }
        if (OpptjeningAktivitetType.NÆRING.equals(periode.getAktivitetType())) {
            if (iaygOpt.isEmpty()) {
                return false;
            }
            var iayg = iaygOpt.get();
            return harGittAksjonspunktForNæring(behandlingId, aktørId, iayg, skjæringstidspunkt);
        }
        return OpptjeningAktivitetType.ANNEN_OPPTJENING.contains(periode.getAktivitetType());
    }

    private boolean harGittAksjonspunktForArbeidsforhold(YrkesaktivitetFilter filter,
                                                         Set<ArbeidType> arbeidTypes,
                                                         BekreftOpptjeningPeriodeDto periode) {
        var yrkesaktiviteter = filter.getYrkesaktiviteter();
        if (yrkesaktiviteter.isEmpty()) {
            return false;
        }
        // Sjekk om 0%
        return yrkesaktiviteter.stream().anyMatch(it -> {
            var erNullProsent = filter.getAktivitetsAvtalerForArbeid(it)
                .stream()
                .anyMatch(aa -> aa.getProsentsats() == null || aa.getProsentsats().erNulltall());
            var erKunstig = it.getArbeidsgiver().getErVirksomhet() && Organisasjonstype.erKunstig(it.getArbeidsgiver().getOrgnr());
            return arbeidTypes.contains(it.getArbeidType()) && it.getArbeidsgiver().getIdentifikator().equals(periode.getArbeidsgiverReferanse()) && (
                erNullProsent || erKunstig);
        });
    }

    private boolean harGittAksjonspunktForNæring(Long behandlingId,
                                                 AktørId aktørId,
                                                 InntektArbeidYtelseGrunnlag iayg,
                                                 Skjæringstidspunkt skjæringstidspunkt) {
        return vurderOpptjening.girAksjonspunktForOppgittNæring(behandlingId, aktørId, iayg, skjæringstidspunkt);
    }

    private DatoIntervallEntitet getPeriode(BekreftOpptjeningPeriodeDto periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getOpptjeningFom(), periode.getOpptjeningTom());
    }

    private YrkesaktivitetBuilder getYrkesaktivitetBuilder(Long behandlingId,
                                                           AktørId aktørId,
                                                           Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag,
                                                           InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder overstyrtBuilder,
                                                           BekreftOpptjeningPeriodeDto periodeDto,
                                                           Set<ArbeidType> arbeidType) {
        if (arbeidType == null || arbeidType.isEmpty()) {
            throw new IllegalStateException("Støtter ikke " + periodeDto.getAktivitetType().getKode());
        }
        if (periodeDto.getAktivitetType().equals(OpptjeningAktivitetType.ARBEID)) {
            return getYrkesaktivitetBuilderForArbeid(behandlingId, aktørId, iayGrunnlag, overstyrtBuilder, periodeDto, arbeidType);
        } else {
            return overstyrtBuilder.getYrkesaktivitetBuilderForType(arbeidType.stream().findFirst().orElse(ArbeidType.UDEFINERT));
        }
    }

    private YrkesaktivitetBuilder getYrkesaktivitetBuilderForArbeid(Long behandlingId,
                                                                    AktørId aktørId,
                                                                    Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag,
                                                                    InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder overstyrtBuilder,
                                                                    BekreftOpptjeningPeriodeDto periodeDto,
                                                                    Set<ArbeidType> arbeidType) {
        var id = periodeDto.getArbeidsforholdRef();
        var ref = id == null ? null : InternArbeidsforholdRef.ref(id);
        var nøkkel = utledOpptjeningsnøkkel(periodeDto, ref);
        var builder = overstyrtBuilder.getYrkesaktivitetBuilderForNøkkelAvType(nøkkel, arbeidType);

        if (!builder.getErOppdatering()) {
            if (Organisasjonstype.erKunstig(periodeDto.getArbeidsgiverReferanse())) {
                kopierVerdierForFiktivtArbeidsforhold(iayGrunnlag, builder);
            } else {
                // Bør få med all informasjon om arbeidsforholdet over i overstyrt slik at
                // ingenting blir mistet.
                builder = getRegisterBuilder(behandlingId, aktørId).getYrkesaktivitetBuilderForNøkkelAvType(nøkkel, arbeidType)
                    .migrerFraRegisterTilOverstyrt();
            }
        }
        builder.medArbeidsforholdId(ref);
        return builder;
    }

    private static Opptjeningsnøkkel utledOpptjeningsnøkkel(BekreftOpptjeningPeriodeDto periodeDto, InternArbeidsforholdRef ref) {
        Opptjeningsnøkkel nøkkel;
        if (OrganisasjonsNummerValidator.erGyldig(periodeDto.getArbeidsgiverReferanse()) || Organisasjonstype.erKunstig(
            periodeDto.getArbeidsgiverReferanse())) {
            nøkkel = new Opptjeningsnøkkel(ref, periodeDto.getArbeidsgiverReferanse(), null);
        } else {
            nøkkel = new Opptjeningsnøkkel(ref, null, periodeDto.getArbeidsgiverReferanse());
        }
        return nøkkel;
    }

    private void kopierVerdierForFiktivtArbeidsforhold(Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag, YrkesaktivitetBuilder builder) {
        var arbeidsforholdInformasjon = iayGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
        if (arbeidsforholdInformasjon.isPresent()) {
            // Det finnes kun en overstyring om vi har et fiktivt arbeidsforhold
            var overstyringer = arbeidsforholdInformasjon.get().getOverstyringer();
            if (overstyringer.size() != 1) {
                throw new IllegalStateException("Fant ingen eller mer enn en overstyring for fiktivt arbeidsforhold");
            }
            var fiktivOverstyring = overstyringer.get(0);
            builder.medArbeidsgiver(fiktivOverstyring.getArbeidsgiver());
            builder.medArbeidsgiverNavn(fiktivOverstyring.getArbeidsgiverNavn());
        }
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder getRegisterBuilder(Long behandlingId, AktørId aktørId) {
        return inntektArbeidYtelseTjeneste.opprettBuilderForRegister(behandlingId).getAktørArbeidBuilder(aktørId);
    }
}
