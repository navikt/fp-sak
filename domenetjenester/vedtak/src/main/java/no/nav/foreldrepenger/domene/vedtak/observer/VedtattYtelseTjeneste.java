package no.nav.foreldrepenger.domene.vedtak.observer;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseStatus;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;

    public VedtattYtelseTjeneste() {
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository,
                                 BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                 BeregningsresultatRepository tilkjentYtelseRepository,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                 FamilieHendelseRepository familieHendelseRepository) {
        this.vedtakRepository = vedtakRepository;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public Ytelse genererYtelse(Behandling behandling, boolean mapArbeidsforhold) {
        final var vedtak = vedtakRepository.hentForBehandling(behandling.getId());
        var berResultat = tilkjentYtelseRepository.hentUtbetBeregningsresultat(behandling.getId());

        final var aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());
        final var ytelse = new YtelseV1();
        ytelse.setFagsystem(Fagsystem.FPSAK);
        ytelse.setKildesystem(Kildesystem.FPSAK);
        ytelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        ytelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        ytelse.setVedtakReferanse(behandling.getUuid().toString());
        ytelse.setAktør(aktør);
        ytelse.setType(map(behandling.getFagsakYtelseType()));
        ytelse.setYtelse(mapYtelser(behandling.getFagsakYtelseType()));
        ytelse.setStatus(map(behandling.getFagsak().getStatus()));
        ytelse.setYtelseStatus(mapStatus(behandling.getFagsak().getStatus()));

        ytelse.setPeriode(utledPeriode(behandling, vedtak, berResultat.orElse(null)));
        ytelse.setAnvist(map(behandling, berResultat.orElse(null), mapArbeidsforhold));
        return ytelse;
    }

    private List<Anvisning> map(Behandling behandling, BeregningsresultatEntitet tilkjentYtelse, boolean mapArbeidsforhold) {
        if (tilkjentYtelse == null) {
            return List.of();
        }
        List<ArbeidsforholdReferanse> arbeidsforholdReferanser = !mapArbeidsforhold ? List.of() :
            inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
                .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon)
                .stream()
                .flatMap(a -> a.getArbeidsforholdReferanser().stream()).collect(Collectors.toList());
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            return !mapArbeidsforhold ? VedtattYtelseMapper.utenArbeidsforhold().mapForeldrepenger(tilkjentYtelse) :
                VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapForeldrepenger(tilkjentYtelse);
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            var beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandling.getId()).orElse(null);
            if (beregningsgrunnlag == null) {
                return List.of();
            }
            // TODO - følg med på TFP-2667 må finne ny metode når Beregning/SVP er skrevet om.
            return !mapArbeidsforhold ? VedtattYtelseMapper.utenArbeidsforhold().mapSvangerskapspenger(tilkjentYtelse, beregningsgrunnlag) :
                VedtattYtelseMapper.medArbeidsforhold(arbeidsforholdReferanser).mapSvangerskapspenger(tilkjentYtelse,beregningsgrunnlag);
        }
        return List.of();
    }


    private Periode utledPeriode(Behandling behandling, BehandlingVedtak vedtak, BeregningsresultatEntitet beregningsresultat) {
        final var periode = new Periode();
        if (beregningsresultat != null) {
            var minFom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
            var maxTom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
                .max(Comparator.naturalOrder());
            if (minFom.isEmpty()) {
                periode.setFom(vedtak.getVedtaksdato());
                periode.setTom(vedtak.getVedtaksdato());
                return periode;
            }
            periode.setFom(minFom.get());
            if (maxTom.isPresent()) {
                periode.setTom(maxTom.get());
            } else {
                periode.setTom(Tid.TIDENES_ENDE);
            }
            return periode;
        } else if (familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId()).isPresent()) {
            try {
                var stp = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                    .map(a -> a.getGjeldendeVersjon().getSkjæringstidspunkt())
                    .orElse(vedtak.getVedtaksdato());
                periode.setFom(stp);
                periode.setTom(stp);
                return periode;
            } catch (Exception e) {
                // papirsøknad elns uten fhdato
            }
        }
        periode.setFom(vedtak.getVedtaksdato());
        periode.setTom(vedtak.getVedtaksdato());
        return periode;
    }


    private YtelseType map(FagsakYtelseType type) {
        return switch (type) {
            case ENGANGSTØNAD -> YtelseType.ENGANGSTØNAD;
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
            default -> throw new IllegalStateException("Ukjent ytelsestype " + type);
        };
    }

    private Ytelser mapYtelser(FagsakYtelseType type) {
        return switch (type) {
            case ENGANGSTØNAD -> Ytelser.ENGANGSTØNAD;
            case FORELDREPENGER -> Ytelser.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelser.SVANGERSKAPSPENGER;
            default -> throw new IllegalStateException("Ukjent ytelsestype " + type);
        };
    }

    private YtelseStatus map(FagsakStatus kode) {
        return switch (kode) {
            case OPPRETTET -> YtelseStatus.OPPRETTET;
            case UNDER_BEHANDLING -> YtelseStatus.UNDER_BEHANDLING;
            case LØPENDE -> YtelseStatus.LØPENDE;
            case AVSLUTTET -> YtelseStatus.AVSLUTTET;
        };
    }

    private Status mapStatus(FagsakStatus kode) {
        return switch (kode) {
            case OPPRETTET, UNDER_BEHANDLING -> Status.UNDER_BEHANDLING;
            case LØPENDE -> Status.LØPENDE;
            case AVSLUTTET -> Status.AVSLUTTET;
        };
    }


}
