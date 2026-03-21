package executor

import (
	"fmt"

	"github.com/architect-platform/architect/internal/core/action"
)

// Node represents an action in the DAG with its resolved dependencies.
type Node struct {
	Action   *action.Action
	Children []*Node // nodes that depend on this one
	InDegree int     // number of unresolved dependencies
}

// DAG is a directed acyclic graph of actions.
type DAG struct {
	Nodes map[string]*Node
	Roots []*Node // nodes with no dependencies (in-degree 0)
}

// BuildDAG constructs a DAG from an action and its transitive dependencies.
func BuildDAG(registry *action.Registry, rootID string) (*DAG, error) {
	dag := &DAG{
		Nodes: make(map[string]*Node),
	}

	// Recursively resolve the action tree
	if err := dag.resolve(registry, rootID); err != nil {
		return nil, err
	}

	// Wire up dependency edges
	for _, node := range dag.Nodes {
		for _, depID := range node.Action.DependsOn {
			dep, ok := dag.Nodes[depID]
			if !ok {
				return nil, fmt.Errorf("action %q depends on %q, which is not in the graph", node.Action.ID, depID)
			}
			dep.Children = append(dep.Children, node)
			node.InDegree++
		}
	}

	// Wire up step ordering: each step depends on the previous one (unless parallel)
	for _, node := range dag.Nodes {
		if len(node.Action.Steps) > 1 && !node.Action.Parallel {
			for i := 1; i < len(node.Action.Steps); i++ {
				prev, prevOK := dag.Nodes[node.Action.Steps[i-1]]
				curr, currOK := dag.Nodes[node.Action.Steps[i]]
				if prevOK && currOK {
					prev.Children = append(prev.Children, curr)
					curr.InDegree++
				}
			}
		}
	}

	// Find roots
	for _, node := range dag.Nodes {
		if node.InDegree == 0 {
			dag.Roots = append(dag.Roots, node)
		}
	}

	// Check for cycles
	if err := dag.detectCycle(); err != nil {
		return nil, err
	}

	return dag, nil
}

// resolve recursively adds an action and its steps/dependencies to the DAG.
func (d *DAG) resolve(registry *action.Registry, id string) error {
	if _, exists := d.Nodes[id]; exists {
		return nil
	}

	a, err := registry.Get(id)
	if err != nil {
		return err
	}

	d.Nodes[id] = &Node{
		Action: a,
	}

	// Resolve steps
	for _, stepID := range a.Steps {
		if err := d.resolve(registry, stepID); err != nil {
			return fmt.Errorf("resolving step %q of %q: %w", stepID, id, err)
		}
	}

	// Resolve dependencies
	for _, depID := range a.DependsOn {
		if err := d.resolve(registry, depID); err != nil {
			return fmt.Errorf("resolving dependency %q of %q: %w", depID, id, err)
		}
	}

	return nil
}

// TopologicalOrder returns actions in valid execution order.
func (d *DAG) TopologicalOrder() []*action.Action {
	// Kahn's algorithm
	inDegree := make(map[string]int)
	for id, node := range d.Nodes {
		inDegree[id] = node.InDegree
	}

	var queue []*Node
	for _, node := range d.Nodes {
		if inDegree[node.Action.ID] == 0 {
			queue = append(queue, node)
		}
	}

	var result []*action.Action
	for len(queue) > 0 {
		node := queue[0]
		queue = queue[1:]
		result = append(result, node.Action)

		for _, child := range node.Children {
			inDegree[child.Action.ID]--
			if inDegree[child.Action.ID] == 0 {
				queue = append(queue, child)
			}
		}
	}

	return result
}

// LeafActions returns actions that actually execute (have Run specs, no steps).
func (d *DAG) LeafActions() []*action.Action {
	var leaves []*action.Action
	for _, node := range d.Nodes {
		if node.Action.Run != nil && len(node.Action.Steps) == 0 {
			leaves = append(leaves, node.Action)
		}
	}
	return leaves
}

func (d *DAG) detectCycle() error {
	visited := make(map[string]int) // 0=unvisited, 1=in-progress, 2=done
	for id := range d.Nodes {
		visited[id] = 0
	}

	var dfs func(id string) error
	dfs = func(id string) error {
		visited[id] = 1
		node := d.Nodes[id]
		for _, child := range node.Children {
			switch visited[child.Action.ID] {
			case 1:
				return fmt.Errorf("cycle detected: %s -> %s", id, child.Action.ID)
			case 0:
				if err := dfs(child.Action.ID); err != nil {
					return err
				}
			}
		}
		visited[id] = 2
		return nil
	}

	for id := range d.Nodes {
		if visited[id] == 0 {
			if err := dfs(id); err != nil {
				return err
			}
		}
	}
	return nil
}
