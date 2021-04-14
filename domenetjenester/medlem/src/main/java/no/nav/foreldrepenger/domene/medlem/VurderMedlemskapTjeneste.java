package no.nav.foreldrepenger.domene.medlem;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.AvklarBarnFødtUtenlands;
import no.nav.foreldrepenger.domene.medlem.impl.AvklarGyldigPeriode;
import no.nav.foreldrepenger.domene.medlem.impl.AvklarOmErBosatt;
import no.nav.foreldrepenger.domene.medlem.impl.AvklarOmSøkerOppholderSegINorge;
import no.nav.foreldrepenger.domene.medlem.impl.AvklaringFaktaMedlemskap;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;

@ApplicationScoped
public class VurderMedlemskapTjeneste {

    private AvklarOmErBosatt avklarOmErBosatt;
    private AvklarGyldigPeriode avklarGyldigPeriode;
    private AvklarBarnFødtUtenlands avklarBarnFødtUtenlands;
    private AvklarOmSøkerOppholderSegINorge avklarOmSøkerOppholderSegINorge;
    private AvklaringFaktaMedlemskap avklaringFaktaMedlemskap;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    protected VurderMedlemskapTjeneste() {
        // CDI
    }

    @Inject
    public VurderMedlemskapTjeneste(BehandlingRepositoryProvider provider,
                                    MedlemskapPerioderTjeneste medlemskapPerioderTjeneste,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste) {
        this.behandlingRepository = provider.getBehandlingRepository();
        this.familieHendelseRepository = provider.getFamilieHendelseRepository();
        this.avklarOmErBosatt = new AvklarOmErBosatt(provider, medlemskapPerioderTjeneste, personopplysningTjeneste);
        this.avklarGyldigPeriode = new AvklarGyldigPeriode(provider, medlemskapPerioderTjeneste);
        this.avklarBarnFødtUtenlands = new AvklarBarnFødtUtenlands(provider);
        this.avklarOmSøkerOppholderSegINorge = new AvklarOmSøkerOppholderSegINorge(provider, personopplysningTjeneste, iayTjeneste);
        this.avklaringFaktaMedlemskap = new AvklaringFaktaMedlemskap(provider, medlemskapPerioderTjeneste, personopplysningTjeneste, iayTjeneste);
    }

    /**
     *
     * @param ref behandlingreferanse
     * @param vurderingsdato hvilken dato vurderingstjenesten skal kjøre for
     * @return Liste med MedlemResultat
     */
    public Set<MedlemResultat> vurderMedlemskap(BehandlingReferanse ref, LocalDate vurderingsdato) {
        var behandlingId = ref.getBehandlingId();
        Set<MedlemResultat> resultat = new HashSet<>();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        avklarOmErBosatt.utled(behandling, vurderingsdato).ifPresent(resultat::add);
        avklarGyldigPeriode.utled(behandlingId, vurderingsdato).ifPresent(resultat::add);
        avklarBarnFødtUtenlands.utled(behandlingId, vurderingsdato).ifPresent(resultat::add);
        if (vurderingsdato.equals(ref.getUtledetSkjæringstidspunkt())) {
            avklarOmSøkerOppholderSegINorge.utled(ref, vurderingsdato).ifPresent(resultat::add);
        }
        avklaringFaktaMedlemskap.utled(ref, behandling, vurderingsdato).ifPresent(resultat::add);
        return resultat;
    }

    public LocalDate beregnVentPåFødselFristTid(BehandlingReferanse ref) {
        return familieHendelseRepository.hentAggregat(ref.getBehandlingId())
            .getGjeldendeTerminbekreftelse().orElseThrow(IllegalStateException::new)
            .getTermindato().plus(Period.parse(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL.getFristPeriode()));
    }
}
