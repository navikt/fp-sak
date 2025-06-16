package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class RevurderingTjenesteImpl implements RevurderingTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private PleiepengerRepository pleiepengerRepository;
    private UføretrygdRepository uføretrygdRepository;
    private NesteSakRepository nesteSakRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private RevurderingEndring revurderingEndring;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VergeRepository vergeRepository;
    private SøknadRepository søknadRepository;
    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;
    private AktivitetskravArbeidRepository aktivitetskravArbeidRepository;

    public RevurderingTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                   BehandlingGrunnlagRepositoryProvider grunnlagProvider,
                                   InntektArbeidYtelseTjeneste iayTjeneste,
                                   @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingEndring revurderingEndring,
                                   RevurderingTjenesteFelles revurderingTjenesteFelles,
                                   VergeRepository vergeRepository) {
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.ytelsesFordelingRepository = grunnlagProvider.getYtelsesFordelingRepository();
        this.familieHendelseRepository = grunnlagProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = grunnlagProvider.getPersonopplysningRepository();
        this.medlemskapRepository = grunnlagProvider.getMedlemskapRepository();
        this.pleiepengerRepository = grunnlagProvider.getPleiepengerRepository();
        this.uføretrygdRepository = grunnlagProvider.getUføretrygdRepository();
        this.nesteSakRepository = grunnlagProvider.getNesteSakRepository();
        this.revurderingEndring = revurderingEndring;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.vergeRepository = vergeRepository;
        this.søknadRepository = grunnlagProvider.getSøknadRepository();
        this.arbeidsforholdValgRepository = grunnlagProvider.getArbeidsforholdValgRepository();
        this.aktivitetskravArbeidRepository = grunnlagProvider.getAktivitetskravArbeidRepository();
    }

    @Override
    public Behandling opprettManuellRevurdering(Fagsak fagsak,
                                                BehandlingÅrsakType revurderingsÅrsak,
                                                OrganisasjonsEnhet enhet,
                                                String opprettetAv) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, true, enhet, opprettetAv);
    }

    @Override
    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak,
                                                   BehandlingÅrsakType revurderingsÅrsak,
                                                   OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, false, enhet, null);
    }

    private Behandling opprettRevurdering(Fagsak fagsak,
                                          BehandlingÅrsakType revurderingsÅrsak,
                                          boolean manueltOpprettet,
                                          OrganisasjonsEnhet enhet, String opprettetAv) {
        var origBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElseThrow(() -> RevurderingFeil.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()));

        // lås original behandling først
        behandlingRepository.taSkriveLås(origBehandling.getId());

        // deretter opprett revurdering
        var revurdering = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingsÅrsak, origBehandling, manueltOpprettet, enhet, opprettetAv);

        // Kopier vilkår (samme vilkår vurderes i Revurdering)
        var revurderingLås = behandlingRepository.taSkriveLås(revurdering.getId());
        revurderingTjenesteFelles.kopierVilkårsresultat(origBehandling, revurdering, revurderingLås);

        // Kopier grunnlagsdata
        this.kopierAlleGrunnlagFraTidligereBehandling(origBehandling, revurdering);

        return revurdering;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        var originalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        pleiepengerRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        uføretrygdRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        nesteSakRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        aktivitetskravArbeidRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        if (BehandlingType.REVURDERING.equals(ny.getType())) {
            ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, ny);
        } else {
            ytelsesFordelingRepository.hentAggregatHvisEksisterer(originalBehandlingId).ifPresent(yfa -> {
                var yfBuilder = YtelseFordelingAggregat.oppdatere(Optional.empty())
                    .medOppgittRettighet(yfa.getOppgittRettighet())
                    .medOppgittDekningsgrad(yfa.getOppgittDekningsgrad())
                    .medOppgittFordeling(revurderingTjenesteFelles.kopierOppgittFordelingFraForrigeBehandling(yfa.getOppgittFordeling()));
                ytelsesFordelingRepository.lagre(nyBehandlingId, yfBuilder.build());
            });
        }
        vergeRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
        arbeidsforholdValgRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandlingTilUtsattSøknad(Behandling original, Behandling ny) {
        var originalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
        uføretrygdRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, nyBehandlingId);

        // Nytt til post-annullering
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(originalBehandlingId);
        var yfBuilder = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittFordeling(ytelseFordelingAggregat.getOppgittFordeling())
            .medOppgittDekningsgrad(ytelseFordelingAggregat.getOppgittDekningsgrad())
            .medOppgittRettighet(ytelseFordelingAggregat.getOppgittRettighet());
        ytelsesFordelingRepository.lagre(nyBehandlingId, yfBuilder.build());
        var originalSøknad = søknadRepository.hentSøknad(originalBehandlingId);
        var søknadBuilder = new SøknadEntitet.Builder(originalSøknad, true).medErEndringssøknad(false);
        søknadRepository.lagreOgFlush(ny, søknadBuilder.build());

        // gjør til slutt, innebærer kall til abakus
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(originalBehandlingId, nyBehandlingId);
    }

    @Override
    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        var sisteVedtakAnnullert = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()))
            .map(Behandlingsresultat::getBehandlingResultatType)
            .filter(BehandlingResultatType.FORELDREPENGER_SENERE::equals)
            .isPresent();
        return !sisteVedtakAnnullert && revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return revurderingEndring.erRevurderingMedUendretUtfall(behandling);
    }
}
