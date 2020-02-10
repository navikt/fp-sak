package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class BeregnTilrettleggingsperioderTjeneste {


    private SvangerskapspengerRepository svangerskapspengerRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private static final Logger LOG = LoggerFactory.getLogger(BeregnTilrettleggingsperioderTjeneste.class);

    public BeregnTilrettleggingsperioderTjeneste() {
        // CDI
    }

    @Inject
    public BeregnTilrettleggingsperioderTjeneste(SvangerskapspengerRepository svangerskapspengerRepository,
                                                     InntektArbeidYtelseTjeneste iayTjeneste,
                                                     FamilieHendelseRepository familieHendelseRepository) {

        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.iayTjeneste = iayTjeneste;
        this.familieHendelseRepository = familieHendelseRepository;
    }


    /** Gir en liste med Arbeidsgiver med arbeidtype som inneholder informasjon om utbetalingsgrad
     *  beregnet basert på register informasjon fra AA-reg og det som har blitt oppgitt i søknaden
     *  for Svangerskapspenger.
     *
     * @param behandlingReferanse {@link BehandlingReferanse}
     * @return liste med Tilrettleggingsperiode {@link TilretteleggingMedUtbelingsgrad} NB! kan gi tom liste hvis det ikke finnes
     * datagrunnlag
     */
    public List<TilretteleggingMedUtbelingsgrad> beregnPerioder(BehandlingReferanse behandlingReferanse) {
        Optional<SvpGrunnlagEntitet> svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingReferanse.getBehandlingId());
        AktørId aktørId = behandlingReferanse.getAktørId();

        if (svpGrunnlagOpt.isPresent()) {
            LocalDate termindato = finnTermindato(behandlingReferanse.getBehandlingId());
            var svpGrunnlag = svpGrunnlagOpt.get();
            var aktuelleTilretteleggingerFiltrert = new TilretteleggingFilter(svpGrunnlag).getAktuelleTilretteleggingerFiltrert();
            Optional<InntektArbeidYtelseGrunnlag> grunnlag = iayTjeneste.finnGrunnlag(behandlingReferanse.getBehandlingId());

            YrkesaktivitetFilter filter = grunnlag.map(g -> new YrkesaktivitetFilter(g.getArbeidsforholdInformasjon(), g.getAktørArbeidFraRegister(aktørId)))
                    .orElse(YrkesaktivitetFilter.EMPTY);

            // beregner i henhold til stillingsprosent på arbeidsforholdene i AA-reg
            var ordinæreArbeidsforhold = aktuelleTilretteleggingerFiltrert.stream()
                .filter(tilrettelegging -> tilrettelegging.getArbeidsgiver().isPresent() && ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.equals(tilrettelegging.getArbeidType()))
                .map(a -> {
                    Collection<AktivitetsAvtale> aktivitetsAvtalerForArbeid = filter.getAktivitetsAvtalerForArbeid(a.getArbeidsgiver().get(), a.getInternArbeidsforholdRef().isPresent() ? a.getInternArbeidsforholdRef().get() : InternArbeidsforholdRef.nullRef(), a.getBehovForTilretteleggingFom());
                    LOG.info("Beregner utbetalingsgrad for arbeidsgiver {} med disse aktivitetene: {}", a.getArbeidsgiver().get().getOrgnr(), aktivitetsAvtalerForArbeid);
                    return UtbetalingsgradBeregner.beregn(aktivitetsAvtalerForArbeid, a, termindato);
                })
                .collect(Collectors.toList());

            // beregner i henhold til stillingsprosent på 100% (Frilans og Selvstendig)
            var frilansOgSelvstendigNæringsdrivende = aktuelleTilretteleggingerFiltrert.stream()
                .filter(tilrettelegging -> tilrettelegging.getArbeidsgiver().isEmpty() && !ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.equals(tilrettelegging.getArbeidType()))
                .map(a -> UtbetalingsgradBeregner.beregnUtenAAreg(a, termindato)).collect(Collectors.toList());

            ordinæreArbeidsforhold.addAll(frilansOgSelvstendigNæringsdrivende);
            if (aktuelleTilretteleggingerFiltrert.size() == ordinæreArbeidsforhold.size()) {
                return ordinæreArbeidsforhold;
            }
            throw new IllegalStateException("Har ikke klart å beregne tilrettleggingsperiodene riktig for behandlingID" + behandlingReferanse.getBehandlingId());
        }
        return Collections.emptyList();
    }

    private LocalDate finnTermindato(Long behandlingId) {
        FamilieHendelseGrunnlagEntitet familiehendelseAggregat = familieHendelseRepository.hentAggregat(behandlingId);
        FamilieHendelseEntitet gjeldendeFamiliehendelse = familiehendelseAggregat.getGjeldendeVersjon();
        Optional<TerminbekreftelseEntitet> terminbekreftelse = gjeldendeFamiliehendelse.getTerminbekreftelse();
        if (terminbekreftelse.isEmpty()) {
            throw new IllegalStateException("Det skal alltid være termindato på svangerskapspenger søknad, gjelder behandlingId=" + behandlingId);
        }
        return terminbekreftelse.get().getTermindato();
    }
}
