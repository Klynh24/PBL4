#ifndef FASTHASH_H
#define FASTHASH_H

#include <memory>
#include <cmath>
#include <cstring>
#include <new>

inline size_t FastHash_Hash(void *ptr)
{
    return (size_t)ptr;
}
inline size_t FastHash_Hash(unsigned int x)
{
    return (size_t)x;
}
inline size_t FastHash_Hash(signed int x)
{
    return (size_t)x;
}

template <typename K, typename V>
class FastHashBase
{
public:
    struct Item
    {
        K key;
        V value;
    };

protected:
    struct ItemNode
    {
        int index;
        ItemNode *pNext;
    };

    typedef ItemNode *ItemNodePtr;

    size_t _fsize;
    size_t _tsize;

    Item *_nodes;
    ItemNode *_itemnodes;
    ItemNode *_freelist;
    ItemNodePtr *_lookuptable;
    int *_indexlist;

    bool _fIndexValid;
    size_t _indexStart;

    size_t _size;

    // Vô hiệu hóa copy constructor và assignment operator
    FastHashBase(const FastHashBase &) = delete;
    FastHashBase &operator=(const FastHashBase &) = delete;

    ItemNode *Find(const K &key, size_t *pHashIndex = nullptr, ItemNode **ppPrev = nullptr)
    {
        size_t hashindex = ((size_t)(FastHash_Hash(key))) % _tsize;
        ItemNode *pPrev = nullptr;
        ItemNode *pProbe = _lookuptable[hashindex];
        while (pProbe)
        {
            if (_nodes[pProbe->index].key == key)
            {
                break;
            }
            pPrev = pProbe;
            pProbe = pProbe->pNext;
        }

        if (pHashIndex)
            *pHashIndex = hashindex;
        if (ppPrev)
            *ppPrev = pPrev;

        return pProbe;
    }

    void ReIndex()
    {
        if ((_indexlist == nullptr) || (_size == 0))
        {
            return;
        }

        size_t index = 0;
        for (size_t t = 0; t < _tsize; ++t)
        {
            ItemNode *pNode = _lookuptable[t];
            while (pNode)
            {
                _indexlist[index++] = pNode->index;
                pNode = pNode->pNext;
            }
        }

        _fIndexValid = true;
        _indexStart = 0;
    }
    void UpdateIndexWithAdd(ItemNode *pNode)
    {
        if (_fIndexValid && (_size < _fsize) && (_indexlist != nullptr))
        {
            size_t pos = (_indexStart + _size) % _fsize;
            _indexlist[pos] = pNode->index;
        }
    }
    void UpdateIndexWithRemove(ItemNode *pNode)
    {
        if ((_size == 0) || (_indexlist == nullptr) || ((_size > 1) && (_fIndexValid == false)))
        {
            return;
        }

        if (_size == 1)
        {
            _fIndexValid = true;
            _indexStart = 0;
            return;
        }

        if (_indexlist[_indexStart] == pNode->index)
        {
            _indexStart = (_indexStart + 1) % _fsize;
            return;
        }

        size_t indexlast = (_indexStart + (_size - 1)) % _fsize;
        if (_indexlist[indexlast] == pNode->index)
        {
            return;
        }

        _fIndexValid = false;
    }

public:
    class iterator
    {
    public:
        using iterator_category = std::forward_iterator_tag;
        using value_type = Item;
        using difference_type = std::ptrdiff_t;
        using pointer = Item *;
        using reference = Item &;

    private:
        FastHashBase<K, V> *_hash;
        ItemNode *_currentNode;

    public:
        iterator(FastHashBase<K, V> *hash, ItemNode *node) : _hash(hash), _currentNode(node) {}

        reference operator*() const
        {
            return _hash->_nodes[_currentNode->index];
        }

        pointer operator->() const
        {
            return &(_hash->_nodes[_currentNode->index]);
        }

        iterator &operator++()
        {
            if (_currentNode == nullptr)
            {
                return *this;
            }
            if (_currentNode->pNext != nullptr)
            {
                _currentNode = _currentNode->pNext;
                return *this;
            }
            size_t currentHashIndex = FastHash_Hash(_hash->_nodes[_currentNode->index].key) % _hash->_tsize;

            _currentNode = nullptr;
            for (size_t i = currentHashIndex + 1; i < _hash->_tsize; ++i)
            {
                if (_hash->_lookuptable[i] != nullptr)
                {
                    _currentNode = _hash->_lookuptable[i];
                    break;
                }
            }
            return *this;
        }

        bool operator!=(const iterator &other) const
        {
            return _currentNode != other._currentNode;
        }
        bool operator==(const iterator &other) const
        {
            return _currentNode == other._currentNode;
        }
    };

    iterator begin()
    {
        if (_size == 0)
        {
            return end();
        }
        for (size_t i = 0; i < _tsize; ++i)
        {
            if (_lookuptable[i] != nullptr)
            {
                return iterator(this, _lookuptable[i]);
            }
        }
        return end();
    }

    iterator end()
    {
        return iterator(this, nullptr);
    }

    FastHashBase()
    {
        Init(0, 0, nullptr, nullptr, nullptr, nullptr);
    }

    void Init(size_t fsize, size_t tsize, Item *nodelist, ItemNode *itemnodelist, ItemNodePtr *table, int *indexlist)
    {
        _fsize = fsize;
        _tsize = tsize;

        _nodes = nodelist;
        _itemnodes = itemnodelist;
        _freelist = nullptr;
        _lookuptable = table;
        _indexlist = indexlist;

        Reset();
    }

    void Reset()
    {
        if (_lookuptable)
        {
            memset(_lookuptable, 0, sizeof(ItemNodePtr) * _tsize);
        }

        if ((_fsize > 0) && (_itemnodes))
        {
            for (size_t x = 0; x < _fsize - 1; x++)
            {
                _itemnodes[x].pNext = &_itemnodes[x + 1];
                _itemnodes[x].index = x;
            }
            _itemnodes[_fsize - 1].pNext = nullptr;
            _itemnodes[_fsize - 1].index = _fsize - 1;
        }

        _freelist = _itemnodes;
        _size = 0;
        _fIndexValid = (_indexlist != nullptr);
        _indexStart = 0;
    }

    size_t Size() const { return _size; }
    size_t GetMaxCapacity() const { return _fsize; }
    size_t GetTableWidth() const { return _tsize; }

    bool IsValid() const
    {
        return ((_tsize > 0) && (_fsize > 0) && (_itemnodes != nullptr) && (_lookuptable != nullptr) && (_nodes != nullptr));
    }

    int Insert(const K &key, const V &value)
    {
        if (_freelist == nullptr)
            return -1;

        size_t hashindex = FastHash_Hash(key) % _tsize;

        ItemNode *pInsert = _freelist;
        _freelist = _freelist->pNext;

        Item *pItem = &_nodes[pInsert->index];
        pItem->key = key;
        pItem->value = value;

        pInsert->pNext = _lookuptable[hashindex];
        _lookuptable[hashindex] = pInsert;

        UpdateIndexWithAdd(pInsert);

        _size++;

        return 1;
    }

    int Remove(const K &key)
    {
        size_t hashindex;
        ItemNode *pPrev = nullptr;
        ItemNode *pNode = Find(key, &hashindex, &pPrev);

        if (pNode == nullptr)
            return -1;

        if (pPrev == nullptr)
        {
            _lookuptable[hashindex] = pNode->pNext;
        }
        else
        {
            pPrev->pNext = pNode->pNext;
        }

        UpdateIndexWithRemove(pNode);

        pNode->pNext = _freelist;
        _freelist = pNode;

        _size--;

        return 1;
    }

    V *Lookup(const K &key)
    {
        ItemNode *pNode = Find(key);
        if (pNode)
        {
            return &(_nodes[pNode->index].value);
        }
        return nullptr;
    }

    bool Exists(const K &key)
    {
        return (Find(key) != nullptr);
    }

    Item *LookupByIndex(size_t index)
    {
        if ((index >= _size) || (_indexlist == nullptr))
        {
            return nullptr;
        }

        if (_fIndexValid == false)
        {
            ReIndex();
            if (_fIndexValid == false)
            {
                return nullptr;
            }
        }

        size_t indexadjusted = (_indexStart + index) % _fsize;
        int itemindex = _indexlist[indexadjusted];
        return &(_nodes[itemindex]);
    }

    V *LookupValueByIndex(size_t index)
    {
        Item *pItem = LookupByIndex(index);
        return pItem ? &pItem->value : nullptr;
    }
};

template <typename K, typename V, size_t FSIZE = 100, size_t TSIZE = 37>
class FastHash : public FastHashBase<K, V>
{
public:
    using Item = typename FastHashBase<K, V>::Item;

private:
    using ItemNode = typename FastHashBase<K, V>::ItemNode;
    using ItemNodePtr = typename FastHashBase<K, V>::ItemNodePtr;

    Item _nodesarray[FSIZE];
    ItemNode _itemnodesarray[FSIZE];
    ItemNodePtr _lookuptablearray[TSIZE];
    int _indexarray[FSIZE];

public:
    FastHash()
    {
        static_assert(FSIZE > 0, "FastHash requires FSIZE > 0");
        static_assert(TSIZE > 0, "FastHash requires TSIZE > 0");

        this->Init(FSIZE, TSIZE, _nodesarray, _itemnodesarray, _lookuptablearray, _indexarray);
    }

    FastHash(const FastHash &) = delete;
    FastHash &operator=(const FastHash &) = delete;
};

template <class K, class V>
class FastHashDynamic : public FastHashBase<K, V>
{
public:
    typedef typename FastHashBase<K, V>::Item Item;

    typename FastHashBase<K, V>::iterator begin()
    {
        return FastHashBase<K, V>::begin();
    }
    typename FastHashBase<K, V>::iterator end()
    {
        return FastHashBase<K, V>::end();
    }

protected:
    typedef typename FastHashBase<K, V>::ItemNode ItemNode;
    typedef typename FastHashBase<K, V>::ItemNodePtr ItemNodePtr;

    std::unique_ptr<Item[]> _nodesarray;
    std::unique_ptr<ItemNode[]> _itemnodesarray;
    std::unique_ptr<ItemNodePtr[]> _lookuptablearray;
    std::unique_ptr<int[]> _indexarray;

public:
    FastHashDynamic() = default;

    FastHashDynamic(size_t fsize, size_t tsize)
    {
        InitTable(fsize, tsize);
    }

    ~FastHashDynamic() = default;

    int InitTable(size_t fsize, size_t tsize);

    void ResetTable()
    {
        _nodesarray.reset();
        _itemnodesarray.reset();
        _lookuptablearray.reset();
        _indexarray.reset();

        this->Init(0, 0, nullptr, nullptr, nullptr, nullptr);
    }
};

size_t FastHash_GetHashTableWidth(unsigned int maxItems);

template <class K, class V>
int FastHashDynamic<K, V>::InitTable(size_t fsize, size_t tsize)
{
    if (fsize == 0)
    {
        return -1;
    }

    if (tsize == 0)
    {
        tsize = FastHash_GetHashTableWidth(static_cast<unsigned int>(fsize));
    }

    ResetTable();

    try
    {
        _nodesarray = std::make_unique<Item[]>(fsize);
        _itemnodesarray = std::make_unique<ItemNode[]>(fsize);
        _lookuptablearray = std::make_unique<ItemNodePtr[]>(tsize);
        _indexarray = std::make_unique<int[]>(fsize);
    }
    catch (const std::bad_alloc &)
    {
        ResetTable();
        return -1;
    }

    this->Init(fsize, tsize, _nodesarray.get(), _itemnodesarray.get(), _lookuptablearray.get(), _indexarray.get());
    return 1;
}

#endif
