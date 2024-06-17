package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;

public class AvklarGyldigPeriode {

    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    public AvklarGyldigPeriode(BehandlingRepositoryProvider repositoryProvider, MedlemskapPerioderTjeneste medlemskapPerioderTjeneste) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.medlemskapPerioderTjeneste = medlemskapPerioderTjeneste;
    }

    public Optional<MedlemResultat> utled(Long behandlingId, LocalDate vurderingsdato) {
        var optPerioder = medlemskapRepository.hentMedlemskap(behandlingId).map(MedlemskapAggregat::getRegistrertMedlemskapPerioder);

        var medlemskapPerioder = optPerioder.orElse(Collections.emptySet());

        // Har bruker treff i gyldig periode hjemlet i §2-9 bokstav a eller c?
        if (harGyldigMedlemsperiodeMedMedlemskap(vurderingsdato, medlemskapPerioder) == JA) {
            return Optional.empty();
        }
        if (harBrukerTreffIMedl(medlemskapPerioder) == NEI) {
            return Optional.empty();
        }
        // Har bruker treff i perioder som er under avklaring eller ikke har start eller sluttdato?
        if (harPeriodeUnderAvklaring(vurderingsdato, medlemskapPerioder) == NEI) {
            return Optional.empty();
        }
        return Optional.of(AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }

    private Utfall harGyldigMedlemsperiodeMedMedlemskap(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        var medlemskapDekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomFrivilligMedlem(medlemskapDekningTyper) ? JA : NEI;
    }

    private Utfall harPeriodeUnderAvklaring(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        return medlemskapPerioderTjeneste.harPeriodeUnderAvklaring(medlemskapPerioder, vurderingsdato) ? JA : NEI;
    }

    private Utfall harBrukerTreffIMedl(Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        return medlemskapPerioder.isEmpty() ? NEI : JA;
    }
}
